package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.documents.BatchUploadItem;
import com.mirceone.inventoryapp.service.documents.BatchUploadResult;
import com.mirceone.inventoryapp.service.documents.DocumentDownload;
import com.mirceone.inventoryapp.service.documents.DocumentService;
import com.mirceone.inventoryapp.service.documents.DocumentSummary;
import com.mirceone.inventoryapp.service.documents.FolderStructureEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    private final UUID userId = UUID.randomUUID();
    private final UUID firmId = UUID.randomUUID();
    private final UUID dossierId = UUID.randomUUID();

    @BeforeEach
    void rateLimitAllows() {
        when(documentUploadRateLimiter.allow(any())).thenReturn(true);
        when(authRateLimiter.allow(any())).thenReturn(true);
    }

    @Test
    void listDocumentsReturnsPage() throws Exception {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        DocumentSummary row = new DocumentSummary(
                docId, firmId, dossierId, "Note.txt", "text/plain", 4, now, userId, "u@example.com",
                "Poze", DocumentProcessingStatus.CLASSIFIED, null
        );
        when(documentService.listDocuments(eq(userId), eq(firmId), eq(dossierId), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/firms/{firmId}/dossiers/{dossierId}/documents", firmId, dossierId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dossierId").value(dossierId.toString()));
    }

    @Test
    void listFolderStructureReturnsTaxonomy() throws Exception {
        when(documentService.listFolderStructure(eq(userId), eq(firmId), eq(dossierId)))
                .thenReturn(List.of(
                        new FolderStructureEntry("Renders", 2),
                        new FolderStructureEntry("Misc", 0)
                ));

        mockMvc.perform(get("/firms/{firmId}/dossiers/{dossierId}/documents/folder-structure", firmId, dossierId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("Renders"))
                .andExpect(jsonPath("$[0].documentCount").value(2));
    }

    @Test
    void batchUploadReturns202() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentService.uploadBatch(eq(userId), eq(firmId), eq(dossierId), any()))
                .thenReturn(new BatchUploadResult(
                        List.of(new BatchUploadItem(docId, "a.png", DocumentProcessingStatus.PENDING)),
                        List.of()
                ));

        MockMultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1, 2});

        mockMvc.perform(multipart("/firms/{firmId}/dossiers/{dossierId}/documents/batch", firmId, dossierId)
                        .file(f1)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isAccepted());
    }

    @Test
    void downloadContentReturnsResource() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentService.openForDownload(eq(userId), eq(firmId), eq(dossierId), eq(docId)))
                .thenReturn(new DocumentDownload(
                        "hello.txt",
                        "text/plain",
                        new ByteArrayResource("body".getBytes(StandardCharsets.UTF_8))
                ));

        mockMvc.perform(get("/firms/{firmId}/dossiers/{dossierId}/documents/{documentId}/content", firmId, dossierId, docId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteDocumentReturns204() throws Exception {
        UUID docId = UUID.randomUUID();
        doNothing().when(documentService).deleteDocument(eq(userId), eq(firmId), eq(dossierId), eq(docId));

        mockMvc.perform(delete("/firms/{firmId}/dossiers/{dossierId}/documents/{documentId}", firmId, dossierId, docId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void serviceForbiddenMapsTo403() throws Exception {
        when(documentService.listDocuments(eq(userId), eq(firmId), eq(dossierId), isNull(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no"));

        mockMvc.perform(get("/firms/{firmId}/dossiers/{dossierId}/documents", firmId, dossierId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isForbidden());
    }
}
