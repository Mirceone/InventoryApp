package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.model.*;
import com.mirceone.inventoryapp.repository.FirmInvitationRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.firms.access.MemberRoleCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class FirmInvitationService {

    private final FirmInvitationRepository firmInvitationRepository;
    private final FirmMemberRepository firmMemberRepository;
    private final FirmRepository firmRepository;
    private final UserRepository userRepository;
    private final FirmAccessService firmAccessService;
    private final FirmInvitationTokenService invitationTokenService;
    private final EmailService emailService;
    private final AuthService authService;
    private final String frontendBaseUrl;
    private final long tokenTtlSeconds;

    public FirmInvitationService(
            FirmInvitationRepository firmInvitationRepository,
            FirmMemberRepository firmMemberRepository,
            FirmRepository firmRepository,
            UserRepository userRepository,
            FirmAccessService firmAccessService,
            FirmInvitationTokenService invitationTokenService,
            EmailService emailService,
            AuthService authService,
            @Value("${app.frontend-url}") String frontendBaseUrl,
            @Value("${app.invitations.token-ttl-seconds:604800}") long tokenTtlSeconds
    ) {
        this.firmInvitationRepository = firmInvitationRepository;
        this.firmMemberRepository = firmMemberRepository;
        this.firmRepository = firmRepository;
        this.userRepository = userRepository;
        this.firmAccessService = firmAccessService;
        this.invitationTokenService = invitationTokenService;
        this.emailService = emailService;
        this.authService = authService;
        this.frontendBaseUrl = stripTrailingSlash(frontendBaseUrl);
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    @Transactional
    public FirmInvitationContracts.CreateInvitationResult createInvitation(
            UUID firmId,
            UUID inviterUserId,
            FirmInvitationContracts.CreateInvitationSpec spec
    ) {
        firmAccessService.requirePermission(firmId, inviterUserId, FirmPermission.MEMBER_MANAGE);
        firmAccessService.requireFirmOperational(firmId);

        if (spec.role() != MemberRole.MEMBER) {
            throw new ResponseStatusException(BAD_REQUEST, "Only MEMBER role can be invited");
        }

        String email = normalizeEmail(spec.email());
        FirmEntity firm = requireFirm(firmId);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (firmMemberRepository.existsByFirmIdAndUserId(firmId, user.getId())) {
                throw new ResponseStatusException(CONFLICT, "User is already a member of this firm");
            }
        });

        firmInvitationRepository.findByFirmIdAndEmailAndStatus(firmId, email, FirmInvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(CONFLICT, "A pending invitation already exists for this email");
                });

        String rawToken = invitationTokenService.generateRawToken();
        String tokenHash = invitationTokenService.hashToken(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(tokenTtlSeconds);

        FirmInvitationEntity invitation = new FirmInvitationEntity(
                firmId, email, MemberRole.MEMBER, tokenHash, inviterUserId, expiresAt
        );
        invitation = firmInvitationRepository.save(invitation);

        String inviteLink = frontendBaseUrl + "/accept-invitation?token=" + rawToken;
        emailService.sendFirmInvitationEmail(
                email,
                firm.getName(),
                inviteLink,
                MemberRoleCatalog.displayLabel(MemberRole.MEMBER)
        );

        return new FirmInvitationContracts.CreateInvitationResult(toSummary(invitation), rawToken);
    }

    @Transactional(readOnly = true)
    public List<FirmInvitationContracts.InvitationSummary> listPendingInvitations(UUID firmId, UUID requesterUserId) {
        firmAccessService.requirePermission(firmId, requesterUserId, FirmPermission.MEMBER_MANAGE);
        expireStaleInvitations();
        return firmInvitationRepository.findAllByFirmIdAndStatusOrderByCreatedAtDesc(firmId, FirmInvitationStatus.PENDING)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void revokeInvitation(UUID firmId, UUID requesterUserId, UUID invitationId) {
        firmAccessService.requirePermission(firmId, requesterUserId, FirmPermission.MEMBER_MANAGE);

        FirmInvitationEntity invitation = firmInvitationRepository.findById(invitationId)
                .filter(inv -> inv.getFirmId().equals(firmId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus() != FirmInvitationStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only pending invitations can be revoked");
        }

        invitation.setStatus(FirmInvitationStatus.REVOKED);
        firmInvitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public FirmInvitationContracts.InvitationPreview previewInvitation(String rawToken) {
        FirmInvitationEntity invitation = resolveActiveInvitation(rawToken);
        FirmEntity firm = requireFirm(invitation.getFirmId());
        boolean accountExists = userRepository.findByEmailIgnoreCase(invitation.getEmail()).isPresent();

        return new FirmInvitationContracts.InvitationPreview(
                firm.getName(),
                invitation.getEmail(),
                maskEmail(invitation.getEmail()),
                invitation.getRole(),
                MemberRoleCatalog.displayLabel(invitation.getRole()),
                invitation.getExpiresAt(),
                accountExists
        );
    }

    @Transactional
    public AuthContracts.IssuedTokenPair acceptInvitation(
            FirmInvitationContracts.AcceptInvitationSpec spec,
            UUID authenticatedUserId
    ) {
        FirmInvitationEntity invitation = resolveActiveInvitation(spec.token());
        String inviteEmail = invitation.getEmail();
        UUID firmId = invitation.getFirmId();

        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(inviteEmail);

        UserEntity user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (authenticatedUserId == null) {
                throw new ResponseStatusException(UNAUTHORIZED, "Login required to accept this invitation");
            }
            if (!authenticatedUserId.equals(user.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "Invitation email does not match logged-in account");
            }
        } else {
            if (authenticatedUserId != null) {
                throw new ResponseStatusException(BAD_REQUEST, "Do not send authorization when creating a new account from invitation");
            }
            validateNewAccountFields(spec);
            user = createLocalUser(inviteEmail, spec.displayName(), spec.password());
        }

        if (firmMemberRepository.existsByFirmIdAndUserId(firmId, user.getId())) {
            throw new ResponseStatusException(CONFLICT, "User is already a member of this firm");
        }

        firmMemberRepository.save(new FirmMemberEntity(firmId, user.getId(), invitation.getRole()));

        invitation.setStatus(FirmInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        firmInvitationRepository.save(invitation);

        return authService.issueTokenPairForUser(user.getId());
    }

    private UserEntity createLocalUser(String email, String displayName, String password) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email already in use");
        }
        return authService.createLocalUser(email, password, displayName);
    }

    private void validateNewAccountFields(FirmInvitationContracts.AcceptInvitationSpec spec) {
        if (spec.displayName() == null || spec.displayName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Display name is required");
        }
        if (spec.displayName().length() > 255) {
            throw new ResponseStatusException(BAD_REQUEST, "Display name is too long");
        }
        if (spec.password() == null || spec.password().length() < 8 || spec.password().length() > 72) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must be between 8 and 72 characters");
        }
    }

    private FirmInvitationEntity resolveActiveInvitation(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invitation token is required");
        }
        expireStaleInvitations();
        String tokenHash = invitationTokenService.hashToken(rawToken.trim());
        FirmInvitationEntity invitation = firmInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired invitation"));

        if (invitation.getStatus() != FirmInvitationStatus.PENDING) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired invitation");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(FirmInvitationStatus.EXPIRED);
            firmInvitationRepository.save(invitation);
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired invitation");
        }
        return invitation;
    }

    private void expireStaleInvitations() {
        Instant now = Instant.now();
        List<FirmInvitationEntity> expired = firmInvitationRepository.findAllByStatusAndExpiresAtBefore(
                FirmInvitationStatus.PENDING,
                now
        );
        for (FirmInvitationEntity inv : expired) {
            inv.setStatus(FirmInvitationStatus.EXPIRED);
            firmInvitationRepository.save(inv);
        }
    }

    private FirmEntity requireFirm(UUID firmId) {
        return firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));
    }

    private FirmInvitationContracts.InvitationSummary toSummary(FirmInvitationEntity invitation) {
        return new FirmInvitationContracts.InvitationSummary(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                MemberRoleCatalog.displayLabel(invitation.getRole()),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(0, at));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
