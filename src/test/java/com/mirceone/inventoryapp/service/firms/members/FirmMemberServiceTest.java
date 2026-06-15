package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmOwnershipTransferConfirmationEntity;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmOwnershipTransferConfirmationRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmMemberServiceTest {

    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private FirmRepository firmRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FirmOwnershipTransferConfirmationRepository ownershipTransferConfirmationRepository;
    @Mock
    private FirmAccessService firmAccessService;
    @Mock
    private EmailService emailService;

    private FirmMemberService firmMemberService;

    private UUID firmId;
    private UUID ownerUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        firmMemberService = new FirmMemberService(
                firmMemberRepository,
                firmRepository,
                userRepository,
                ownershipTransferConfirmationRepository,
                firmAccessService,
                emailService,
                new AfterCommitExecutor(),
                900
        );
        firmId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();
    }

    @Test
    void updateMemberRoleRejectsOwnerAssignmentThroughGenericRoute() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmMemberService.updateMemberRole(
                        firmId,
                        ownerUserId,
                        memberUserId,
                        new FirmMemberContracts.UpdateMemberRoleSpec(MemberRole.OWNER)
                )
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void updateMemberRoleReturnsUpdatedMemberSummary() {
        FirmMemberEntity member = new FirmMemberEntity(firmId, memberUserId, MemberRole.MEMBER);
        ReflectionTestUtils.setField(member, "createdAt", java.time.Instant.parse("2026-05-25T08:00:00Z"));
        UserEntity user = new UserEntity("member@example.com", "hash", ProviderType.LOCAL, "member@example.com", "Member");
        ReflectionTestUtils.setField(user, "id", memberUserId);

        when(firmMemberRepository.findByFirmIdAndUserId(firmId, memberUserId)).thenReturn(Optional.of(member));
        when(firmMemberRepository.save(any(FirmMemberEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllById(List.of(memberUserId))).thenReturn(List.of(user));

        FirmMemberContracts.FirmMemberSummary summary = firmMemberService.updateMemberRole(
                firmId,
                ownerUserId,
                memberUserId,
                new FirmMemberContracts.UpdateMemberRoleSpec(MemberRole.MEMBER)
        );

        assertEquals(memberUserId, summary.userId());
        assertEquals("member@example.com", summary.email());
        assertEquals("Angajat", summary.roleDisplayLabel());
    }

    @Test
    void removeMemberRejectsCurrentOwner() {
        FirmMemberEntity owner = new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER);
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmMemberService.removeMember(firmId, ownerUserId, ownerUserId)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void requestOwnershipTransferCreatesPendingConfirmation() {
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo");
        ReflectionTestUtils.setField(firm, "id", firmId);
        FirmMemberEntity currentOwner = new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER);
        FirmMemberEntity nextOwner = new FirmMemberEntity(firmId, memberUserId, MemberRole.MEMBER);
        UserEntity owner = new UserEntity("owner@example.com", "hash", ProviderType.LOCAL, "owner@example.com", "Owner");
        UserEntity member = new UserEntity("member@example.com", "hash", ProviderType.LOCAL, "member@example.com", "Member");
        ReflectionTestUtils.setField(owner, "id", ownerUserId);
        ReflectionTestUtils.setField(member, "id", memberUserId);

        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.of(currentOwner));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, memberUserId)).thenReturn(Optional.of(nextOwner));
        when(firmMemberRepository.countByFirmIdAndRole(firmId, MemberRole.OWNER)).thenReturn(1L);
        when(userRepository.findById(ownerUserId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(member));

        FirmMemberContracts.TransferOwnershipRequestResult result = firmMemberService.requestOwnershipTransfer(
                firmId,
                ownerUserId,
                new FirmMemberContracts.TransferOwnershipSpec(memberUserId)
        );

        assertEquals(memberUserId, result.newOwnerUserId());
        assertEquals(6, result.rawConfirmationCode().length());
        verify(ownershipTransferConfirmationRepository).save(any(FirmOwnershipTransferConfirmationEntity.class));
    }

    @Test
    void confirmOwnershipTransferSyncsRolesAndFirmOwnerReference() {
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo");
        ReflectionTestUtils.setField(firm, "id", firmId);
        FirmMemberEntity currentOwner = new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER);
        FirmMemberEntity nextOwner = new FirmMemberEntity(firmId, memberUserId, MemberRole.MEMBER);
        UserEntity owner = new UserEntity("owner@example.com", "hash", ProviderType.LOCAL, "owner@example.com", "Owner");
        UserEntity member = new UserEntity("member@example.com", "hash", ProviderType.LOCAL, "member@example.com", "Member");
        ReflectionTestUtils.setField(owner, "id", ownerUserId);
        ReflectionTestUtils.setField(member, "id", memberUserId);
        String code = "123456";
        FirmOwnershipTransferConfirmationEntity confirmation = new FirmOwnershipTransferConfirmationEntity(
                firmId,
                ownerUserId,
                memberUserId,
                com.mirceone.inventoryapp.service.auth.PasswordResetTokenService.hashToken(code),
                java.time.Instant.now().plusSeconds(900)
        );

        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.of(currentOwner));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, memberUserId)).thenReturn(Optional.of(nextOwner));
        when(firmMemberRepository.countByFirmIdAndRole(firmId, MemberRole.OWNER)).thenReturn(1L);
        when(userRepository.findById(ownerUserId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(member));
        when(ownershipTransferConfirmationRepository
                .findFirstByFirmIdAndRequesterUserIdAndNewOwnerUserIdOrderByCreatedAtDesc(firmId, ownerUserId, memberUserId))
                .thenReturn(Optional.of(confirmation));

        firmMemberService.confirmOwnershipTransfer(
                firmId,
                ownerUserId,
                new FirmMemberContracts.ConfirmOwnershipTransferSpec(memberUserId, code)
        );

        assertEquals(MemberRole.MEMBER, currentOwner.getRole());
        assertEquals(MemberRole.OWNER, nextOwner.getRole());
        assertEquals(memberUserId, firm.getOwnerUserId());
        verify(firmRepository).save(firm);
        verify(ownershipTransferConfirmationRepository).delete(confirmation);
    }

    @Test
    void requestOwnershipTransferRejectsInconsistentOwnerState() {
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo");
        ReflectionTestUtils.setField(firm, "id", firmId);
        FirmMemberEntity currentOwner = new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER);
        FirmMemberEntity nextOwner = new FirmMemberEntity(firmId, memberUserId, MemberRole.MEMBER);
        UserEntity owner = new UserEntity("owner@example.com", "hash", ProviderType.LOCAL, "owner@example.com", "Owner");
        UserEntity member = new UserEntity("member@example.com", "hash", ProviderType.LOCAL, "member@example.com", "Member");
        ReflectionTestUtils.setField(owner, "id", ownerUserId);
        ReflectionTestUtils.setField(member, "id", memberUserId);
        ReflectionTestUtils.setField(firm, "ownerUserId", UUID.randomUUID());

        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.of(currentOwner));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, memberUserId)).thenReturn(Optional.of(nextOwner));
        when(userRepository.findById(ownerUserId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(memberUserId)).thenReturn(Optional.of(member));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmMemberService.requestOwnershipTransfer(
                        firmId,
                        ownerUserId,
                        new FirmMemberContracts.TransferOwnershipSpec(memberUserId)
                )
        );

        assertEquals(409, ex.getStatusCode().value());
    }
}
