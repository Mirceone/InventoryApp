package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmOwnershipTransferConfirmationEntity;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmOwnershipTransferConfirmationRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.PasswordResetTokenService;
import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.firms.access.MemberRoleCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FirmMemberService {

    private final FirmMemberRepository firmMemberRepository;
    private final FirmRepository firmRepository;
    private final UserRepository userRepository;
    private final FirmOwnershipTransferConfirmationRepository ownershipTransferConfirmationRepository;
    private final FirmAccessService firmAccessService;
    private final EmailService emailService;
    private final long ownershipTransferConfirmationTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public FirmMemberService(
            FirmMemberRepository firmMemberRepository,
            FirmRepository firmRepository,
            UserRepository userRepository,
            FirmOwnershipTransferConfirmationRepository ownershipTransferConfirmationRepository,
            FirmAccessService firmAccessService,
            EmailService emailService,
            @Value("${app.firms.ownership-transfer-confirmation-ttl-seconds:900}") long ownershipTransferConfirmationTtlSeconds
    ) {
        this.firmMemberRepository = firmMemberRepository;
        this.firmRepository = firmRepository;
        this.userRepository = userRepository;
        this.ownershipTransferConfirmationRepository = ownershipTransferConfirmationRepository;
        this.firmAccessService = firmAccessService;
        this.emailService = emailService;
        this.ownershipTransferConfirmationTtlSeconds = ownershipTransferConfirmationTtlSeconds;
    }

    @Transactional(readOnly = true)
    public List<FirmMemberContracts.FirmMemberSummary> listMembers(UUID firmId, UUID requesterUserId) {
        firmAccessService.requirePermission(firmId, requesterUserId, FirmPermission.MEMBER_MANAGE);

        List<FirmMemberEntity> members = firmMemberRepository.findAllByFirmIdOrderByCreatedAtAsc(firmId);
        Map<UUID, UserEntity> usersById = loadUsersById(members);

        List<FirmMemberContracts.FirmMemberSummary> result = new ArrayList<>();
        for (FirmMemberEntity member : members) {
            result.add(toSummary(member, usersById));
        }
        return result;
    }

    @Transactional
    public FirmMemberContracts.FirmMemberSummary updateMemberRole(
            UUID firmId,
            UUID requesterUserId,
            UUID memberUserId,
            FirmMemberContracts.UpdateMemberRoleSpec spec
    ) {
        firmAccessService.requirePermission(firmId, requesterUserId, FirmPermission.MEMBER_MANAGE);
        firmAccessService.requireFirmOperational(firmId);

        if (spec.role() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Member role is required");
        }
        if (spec.role() == MemberRole.OWNER) {
            throw new ResponseStatusException(BAD_REQUEST, "Use ownership transfer to assign OWNER role");
        }

        FirmMemberEntity member = requireMember(firmId, memberUserId);
        if (member.getRole() == MemberRole.OWNER) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot change the current owner role directly");
        }

        member.setRole(spec.role());
        FirmMemberEntity saved = firmMemberRepository.save(member);
        return toSummary(saved, loadUsersById(List.of(saved)));
    }

    @Transactional
    public void removeMember(UUID firmId, UUID requesterUserId, UUID memberUserId) {
        firmAccessService.requirePermission(firmId, requesterUserId, FirmPermission.MEMBER_MANAGE);
        firmAccessService.requireFirmOperational(firmId);

        FirmMemberEntity member = requireMember(firmId, memberUserId);
        if (member.getRole() == MemberRole.OWNER) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot remove the current owner");
        }

        firmMemberRepository.delete(member);
    }

    @Transactional
    public FirmMemberContracts.TransferOwnershipRequestResult requestOwnershipTransfer(
            UUID firmId,
            UUID requesterUserId,
            FirmMemberContracts.TransferOwnershipSpec spec
    ) {
        TransferContext context = prepareTransferContext(firmId, requesterUserId, spec.newOwnerUserId());
        cleanupExpiredOwnershipTransferConfirmations();
        ownershipTransferConfirmationRepository.deleteAllByFirmIdAndRequesterUserId(firmId, requesterUserId);

        String rawCode = generateConfirmationCode();
        Instant expiresAt = Instant.now().plusSeconds(ownershipTransferConfirmationTtlSeconds);
        ownershipTransferConfirmationRepository.save(new FirmOwnershipTransferConfirmationEntity(
                firmId,
                requesterUserId,
                spec.newOwnerUserId(),
                PasswordResetTokenService.hashToken(rawCode),
                expiresAt
        ));

        scheduleAfterCommit(() -> emailService.sendOwnershipTransferConfirmationCodeEmail(
                context.currentOwnerUser().getEmail(),
                context.firm().getName(),
                displayNameOrEmail(context.newOwnerUser()),
                rawCode,
                ttlMinutes()
        ));

        return new FirmMemberContracts.TransferOwnershipRequestResult(spec.newOwnerUserId(), expiresAt, rawCode);
    }

    @Transactional
    public void confirmOwnershipTransfer(
            UUID firmId,
            UUID requesterUserId,
            FirmMemberContracts.ConfirmOwnershipTransferSpec spec
    ) {
        if (spec.confirmationCode() == null || spec.confirmationCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Confirmation code is required");
        }
        TransferContext context = prepareTransferContext(firmId, requesterUserId, spec.newOwnerUserId());
        cleanupExpiredOwnershipTransferConfirmations();

        FirmOwnershipTransferConfirmationEntity confirmation = ownershipTransferConfirmationRepository
                .findFirstByFirmIdAndRequesterUserIdAndNewOwnerUserIdOrderByCreatedAtDesc(
                        firmId,
                        requesterUserId,
                        spec.newOwnerUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid or expired ownership transfer confirmation code"));

        if (confirmation.getExpiresAt().isBefore(Instant.now())) {
            ownershipTransferConfirmationRepository.delete(confirmation);
            throw new ResponseStatusException(BAD_REQUEST, "Invalid or expired ownership transfer confirmation code");
        }

        String providedHash = PasswordResetTokenService.hashToken(spec.confirmationCode().strip());
        if (!confirmation.getCodeHash().equals(providedHash)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid or expired ownership transfer confirmation code");
        }

        ownershipTransferConfirmationRepository.delete(confirmation);
        performOwnershipTransfer(context.firm(), context.currentOwnerMember(), context.newOwnerMember());

        scheduleAfterCommit(() -> {
            emailService.sendOwnershipTransferCompletedForPreviousOwnerEmail(
                    context.currentOwnerUser().getEmail(),
                    context.firm().getName(),
                    displayNameOrEmail(context.newOwnerUser())
            );
            emailService.sendOwnershipTransferCompletedForNewOwnerEmail(
                    context.newOwnerUser().getEmail(),
                    context.firm().getName(),
                    displayNameOrEmail(context.currentOwnerUser())
            );
        });
    }

    private FirmEntity requireFirm(UUID firmId) {
        return firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));
    }

    private FirmMemberEntity requireMember(UUID firmId, UUID memberUserId) {
        return firmMemberRepository.findByFirmIdAndUserId(firmId, memberUserId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Member not found"));
    }

    private Map<UUID, UserEntity> loadUsersById(List<FirmMemberEntity> members) {
        List<UUID> userIds = members.stream().map(FirmMemberEntity::getUserId).toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private TransferContext prepareTransferContext(UUID firmId, UUID requesterUserId, UUID newOwnerUserId) {
        firmAccessService.requirePermission(firmId, requesterUserId, FirmPermission.MEMBER_MANAGE);
        firmAccessService.requireFirmOperational(firmId);

        if (newOwnerUserId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "New owner userId is required");
        }
        if (requesterUserId.equals(newOwnerUserId)) {
            throw new ResponseStatusException(BAD_REQUEST, "The selected user is already the current owner");
        }

        FirmEntity firm = requireFirm(firmId);
        FirmMemberEntity currentOwner = requireMember(firmId, requesterUserId);
        if (currentOwner.getRole() != MemberRole.OWNER) {
            throw new ResponseStatusException(BAD_REQUEST, "Only the current owner can transfer ownership");
        }

        FirmMemberEntity nextOwner = requireMember(firmId, newOwnerUserId);
        if (nextOwner.getRole() == MemberRole.OWNER) {
            throw new ResponseStatusException(BAD_REQUEST, "The selected user is already the current owner");
        }

        UserEntity currentOwnerUser = requireUser(requesterUserId);
        UserEntity newOwnerUser = requireUser(newOwnerUserId);
        return new TransferContext(firm, currentOwner, nextOwner, currentOwnerUser, newOwnerUser);
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    private void performOwnershipTransfer(FirmEntity firm, FirmMemberEntity currentOwner, FirmMemberEntity nextOwner) {
        currentOwner.setRole(MemberRole.MEMBER);
        nextOwner.setRole(MemberRole.OWNER);
        firm.setOwnerUserId(nextOwner.getUserId());

        firmMemberRepository.save(currentOwner);
        firmMemberRepository.save(nextOwner);
        firmRepository.save(firm);
    }

    private void cleanupExpiredOwnershipTransferConfirmations() {
        ownershipTransferConfirmationRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private String generateConfirmationCode() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private long ttlMinutes() {
        return Math.max(1L, Math.ceilDiv(ownershipTransferConfirmationTtlSeconds, 60L));
    }

    private void scheduleAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private static String displayNameOrEmail(UserEntity user) {
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName.strip();
        }
        return user.getEmail();
    }

    private FirmMemberContracts.FirmMemberSummary toSummary(FirmMemberEntity member, Map<UUID, UserEntity> usersById) {
        UserEntity user = usersById.get(member.getUserId());
        if (user == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found for member");
        }
        return new FirmMemberContracts.FirmMemberSummary(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                member.getRole(),
                MemberRoleCatalog.displayLabel(member.getRole()),
                member.getCreatedAt()
        );
    }

    private record TransferContext(
            FirmEntity firm,
            FirmMemberEntity currentOwnerMember,
            FirmMemberEntity newOwnerMember,
            UserEntity currentOwnerUser,
            UserEntity newOwnerUser
    ) {
    }
}
