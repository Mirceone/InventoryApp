package com.mirceone.inventoryapp.api.firms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FirmController.class)
class FirmControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FirmService firmService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    private static FirmContracts.FirmSummary summary(
            UUID id, String name, MemberRole role, FirmStatus status, String statusMessage
    ) {
        String roleLabel = role == MemberRole.OWNER ? "Admin" : "Angajat";
        String statusLabel = switch (status) {
            case ACTIVE -> "Activ";
            case PAUSED -> "În pauză";
            case CRITICAL -> "Critic";
        };
        return new FirmContracts.FirmSummary(id, name, role, roleLabel, status, statusLabel, statusMessage);
    }

    @Test
    void createFirmReturnsCreatedFirm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();

        when(firmService.createFirm(eq(userId), any(FirmContracts.CreateFirmSpec.class)))
                .thenReturn(summary(firmId, "Demo SRL", MemberRole.OWNER, FirmStatus.ACTIVE, null));

        mockMvc.perform(post("/firms")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFirmRequest("Demo SRL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusDisplayLabel").value("Activ"));
    }

    @Test
    void listFirmsReturnsStatusFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId1 = UUID.randomUUID();
        UUID firmId2 = UUID.randomUUID();

        when(firmService.getFirmsForUser(userId)).thenReturn(List.of(
                summary(firmId1, "Firm One", MemberRole.OWNER, FirmStatus.ACTIVE, null),
                summary(firmId2, "Firm Two", MemberRole.MEMBER, FirmStatus.PAUSED, null)
        ));

        mockMvc.perform(get("/firms")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].status").value("PAUSED"))
                .andExpect(jsonPath("$[1].statusDisplayLabel").value("În pauză"));
    }

    @Test
    void updateFirmStatusReturnsUpdatedFirm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();

        when(firmService.updateFirmStatus(eq(userId), eq(firmId), any(FirmContracts.UpdateFirmStatusSpec.class)))
                .thenReturn(summary(firmId, "Demo", MemberRole.OWNER, FirmStatus.CRITICAL, "DB issue"));

        mockMvc.perform(patch("/firms/{firmId}/status", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CRITICAL","message":"DB issue"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CRITICAL"))
                .andExpect(jsonPath("$.statusMessage").value("DB issue"));
    }

    @Test
    void deleteFirmReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();

        mockMvc.perform(delete("/firms/{firmId}", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }
}
