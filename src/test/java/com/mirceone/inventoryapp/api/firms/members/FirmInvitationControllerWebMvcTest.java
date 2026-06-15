package com.mirceone.inventoryapp.api.firms.members;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.model.FirmInvitationStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.firms.members.FirmInvitationContracts;
import com.mirceone.inventoryapp.service.firms.members.FirmInvitationService;
import com.mirceone.inventoryapp.service.firms.members.FirmMemberContracts;
import com.mirceone.inventoryapp.service.firms.members.FirmMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FirmInvitationController.class)
class FirmInvitationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FirmInvitationService firmInvitationService;

    @MockitoBean
    private FirmMemberService firmMemberService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void createInvitationReturnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        when(firmInvitationService.createInvitation(eq(firmId), eq(userId), any()))
                .thenReturn(new FirmInvitationContracts.CreateInvitationResult(new FirmInvitationContracts.InvitationSummary(
                        invitationId,
                        "member@example.com",
                        MemberRole.MEMBER,
                        "Angajat",
                        FirmInvitationStatus.PENDING,
                        Instant.parse("2026-12-31T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z")
                ), "raw-token-not-exposed"));

        mockMvc.perform(post("/firms/{firmId}/invitations", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteMemberRequest("member@example.com", MemberRole.MEMBER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void listPendingInvitationsReturnsList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();

        when(firmInvitationService.listPendingInvitations(firmId, userId)).thenReturn(List.of());

        mockMvc.perform(get("/firms/{firmId}/invitations", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listMembersReturnsMemberList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();

        when(firmMemberService.listMembers(firmId, userId)).thenReturn(List.of(
                new FirmMemberContracts.FirmMemberSummary(
                        memberUserId,
                        "member@example.com",
                        "Member",
                        MemberRole.MEMBER,
                        "Angajat",
                        Instant.parse("2026-01-01T00:00:00Z")
                )
        ));

        mockMvc.perform(get("/firms/{firmId}/members", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(memberUserId.toString()))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));
    }

    @Test
    void updateMemberRoleReturnsUpdatedMember() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();

        when(firmMemberService.updateMemberRole(eq(firmId), eq(userId), eq(memberUserId), any()))
                .thenReturn(new FirmMemberContracts.FirmMemberSummary(
                        memberUserId,
                        "member@example.com",
                        "Member",
                        MemberRole.MEMBER,
                        "Angajat",
                        Instant.parse("2026-01-01T00:00:00Z")
                ));

        mockMvc.perform(patch("/firms/{firmId}/members/{memberUserId}/role", firmId, memberUserId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMemberRoleRequest(MemberRole.MEMBER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(memberUserId.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void removeMemberReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();

        mockMvc.perform(delete("/firms/{firmId}/members/{memberUserId}", firmId, memberUserId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void revokeInvitationReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        doNothing().when(firmInvitationService).revokeInvitation(firmId, userId, invitationId);

        mockMvc.perform(delete("/firms/{firmId}/invitations/{invitationId}", firmId, invitationId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void requestOwnershipTransferReturnsAccepted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID newOwnerUserId = UUID.randomUUID();

        when(firmMemberService.requestOwnershipTransfer(eq(firmId), eq(userId), any()))
                .thenReturn(new FirmMemberContracts.TransferOwnershipRequestResult(
                        newOwnerUserId,
                        Instant.parse("2026-01-01T00:10:00Z"),
                        "123456"
                ));

        mockMvc.perform(post("/firms/{firmId}/ownership/transfer", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferOwnershipRequest(newOwnerUserId))))
                .andExpect(status().isAccepted());
    }

    @Test
    void confirmOwnershipTransferReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID newOwnerUserId = UUID.randomUUID();
        doNothing().when(firmMemberService).confirmOwnershipTransfer(eq(firmId), eq(userId), any());

        mockMvc.perform(post("/firms/{firmId}/ownership/transfer/confirm", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmOwnershipTransferRequest(newOwnerUserId, "123456"))))
                .andExpect(status().isNoContent());
    }
}
