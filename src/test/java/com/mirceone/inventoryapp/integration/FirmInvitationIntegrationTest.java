package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.firms.members.FirmInvitationContracts;
import com.mirceone.inventoryapp.service.firms.members.FirmInvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FirmInvitationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private FirmInvitationService firmInvitationService;
    @Autowired
    private FirmMemberRepository firmMemberRepository;

    @Test
    void inviteNewUserAcceptCreatesMemberAndTokens() {
        authService.signup(new AuthContracts.SignupSpec("owner-invite@example.com", "password123", "Owner"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("owner-invite@example.com").orElseThrow();
        FirmContracts.FirmSummary firm =
                firmService.createFirm(owner.getId(), new FirmContracts.CreateFirmSpec("Invite Firm"));

        FirmInvitationContracts.CreateInvitationResult created = firmInvitationService.createInvitation(
                firm.id(),
                owner.getId(),
                new FirmInvitationContracts.CreateInvitationSpec("new-member@example.com", MemberRole.MEMBER)
        );
        assertEquals(MemberRole.MEMBER, created.summary().role());

        FirmInvitationContracts.InvitationPreview preview =
                firmInvitationService.previewInvitation(created.rawToken());
        assertEquals("Invite Firm", preview.firmName());
        assertFalse(preview.accountExists());

        AuthContracts.IssuedTokenPair tokens = firmInvitationService.acceptInvitation(
                new FirmInvitationContracts.AcceptInvitationSpec(created.rawToken(), "New Member", "password456"),
                null
        );
        assertNotNull(tokens.accessToken());

        UserEntity member = userRepository.findByEmailIgnoreCase("new-member@example.com").orElseThrow();
        assertEquals("New Member", member.getDisplayName());
        assertTrue(firmMemberRepository.existsByFirmIdAndUserId(firm.id(), member.getId()));

        var firms = firmService.getFirmsForUser(member.getId());
        assertEquals(1, firms.size());
        assertEquals("Invite Firm", firms.getFirst().name());
        assertEquals(MemberRole.MEMBER, firms.getFirst().role());
    }

    @Test
    void existingUserMustLoginBeforeAccept() {
        authService.signup(new AuthContracts.SignupSpec("owner2-invite@example.com", "password123", "Owner"));
        authService.signup(new AuthContracts.SignupSpec("existing@example.com", "password123", "Existing"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("owner2-invite@example.com").orElseThrow();
        FirmContracts.FirmSummary firm =
                firmService.createFirm(owner.getId(), new FirmContracts.CreateFirmSpec("Firm Two"));

        FirmInvitationContracts.CreateInvitationResult created = firmInvitationService.createInvitation(
                firm.id(),
                owner.getId(),
                new FirmInvitationContracts.CreateInvitationSpec("existing@example.com", MemberRole.MEMBER)
        );

        assertTrue(firmInvitationService.previewInvitation(created.rawToken()).accountExists());

        assertThrows(ResponseStatusException.class, () -> firmInvitationService.acceptInvitation(
                new FirmInvitationContracts.AcceptInvitationSpec(created.rawToken(), null, null),
                null
        ));

        UserEntity existing = userRepository.findByEmailIgnoreCase("existing@example.com").orElseThrow();
        firmInvitationService.acceptInvitation(
                new FirmInvitationContracts.AcceptInvitationSpec(created.rawToken(), null, null),
                existing.getId()
        );
        assertTrue(firmMemberRepository.existsByFirmIdAndUserId(firm.id(), existing.getId()));
    }
}
