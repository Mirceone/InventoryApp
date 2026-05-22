package com.mirceone.inventoryapp.api.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.documents.DossierService;
import com.mirceone.inventoryapp.service.documents.DossierSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DossierController.class)
class DossierControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DossierService dossierService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void createDossierReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        when(dossierService.createDossier(eq(userId), eq(firmId), eq("Proiect X")))
                .thenReturn(new DossierSummary(
                        dossierId, firmId, "Proiect X", userId, Instant.parse("2026-01-01T00:00:00Z"), 0
                ));

        mockMvc.perform(post("/firms/{firmId}/dossiers", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDossierRequest("Proiect X"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Proiect X"));
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        when(dossierService.createDossier(eq(userId), eq(firmId), eq("Dup")))
                .thenThrow(new ResponseStatusException(CONFLICT, "duplicate"));

        mockMvc.perform(post("/firms/{firmId}/dossiers", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDossierRequest("Dup"))))
                .andExpect(status().isConflict());
    }

    @Test
    void listDossiersReturnsArray() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        when(dossierService.listDossiers(eq(userId), eq(firmId)))
                .thenReturn(List.of(new DossierSummary(
                        UUID.randomUUID(), firmId, "A", userId, Instant.now(), 3
                )));

        mockMvc.perform(get("/firms/{firmId}/dossiers", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentCount").value(3));
    }

    @Test
    void deleteDossierReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        doNothing().when(dossierService).deleteDossier(eq(userId), eq(firmId), eq(dossierId));

        mockMvc.perform(delete("/firms/{firmId}/dossiers/{dossierId}", firmId, dossierId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }
}
