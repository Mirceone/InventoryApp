package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.model.*;
import com.mirceone.inventoryapp.repository.FirmInvitationRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirmInvitationServiceTest {

    @Mock
    private FirmInvitationRepository firmInvitationRepository;
    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private FirmRepository firmRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FirmAccessService firmAccessService;
    @Mock
    private FirmInvitationTokenService invitationTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private AuthService authService;

    private FirmInvitationService service;

    private final UUID firmId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FirmInvitationService(
                firmInvitationRepository,
                firmMemberRepository,
                firmRepository,
                userRepository,
                firmAccessService,
                invitationTokenService,
                emailService,
                authService,
                new AfterCommitExecutor(),
                "http://localhost:5173",
                604800L
        );
    }

    @Test
    void createInvitationRejectsOwnerRole() {
        assertThrows(ResponseStatusException.class, () ->
                service.createInvitation(
                        firmId,
                        ownerId,
                        new FirmInvitationContracts.CreateInvitationSpec("a@b.com", MemberRole.OWNER)
                )
        );
        verify(firmInvitationRepository, never()).save(any(FirmInvitationEntity.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void createInvitationSendsEmailForMember() {
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(new FirmEntity(ownerId, "Acme")));
        when(userRepository.findByEmailIgnoreCase("invitee@example.com")).thenReturn(Optional.empty());
        when(firmInvitationRepository.findByFirmIdAndEmailAndStatus(
                firmId, "invitee@example.com", FirmInvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitationTokenService.generateRawToken()).thenReturn("raw-token");
        when(invitationTokenService.hashToken("raw-token")).thenReturn("hashed");
        when(firmInvitationRepository.save(any(FirmInvitationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FirmInvitationContracts.CreateInvitationResult result = service.createInvitation(
                firmId,
                ownerId,
                new FirmInvitationContracts.CreateInvitationSpec("Invitee@Example.com", MemberRole.MEMBER)
        );

        assertEquals("invitee@example.com", result.summary().email());
        assertEquals(MemberRole.MEMBER, result.summary().role());
        verify(emailService).sendFirmInvitationEmail(
                eq("invitee@example.com"),
                eq("Acme"),
                eq("http://localhost:5173/accept-invitation?token=raw-token"),
                eq("Angajat")
        );
    }

    @Test
    void maskEmailHidesLocalPart() {
        assertEquals("i***@example.com", FirmInvitationService.maskEmail("invitee@example.com"));
    }

    @Test
    void previewExpiredInvitationDoesNotPersistState() {
        FirmInvitationEntity invitation = new FirmInvitationEntity(
                firmId,
                "invitee@example.com",
                MemberRole.MEMBER,
                "hashed-token",
                ownerId,
                Instant.now().minusSeconds(60)
        );
        when(invitationTokenService.hashToken("expired-token")).thenReturn("hashed-token");
        when(firmInvitationRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(invitation));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.previewInvitation("expired-token")
        );

        assertEquals(401, ex.getStatusCode().value());
        verify(firmInvitationRepository, never()).save(any(FirmInvitationEntity.class));
    }
}
