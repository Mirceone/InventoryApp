package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.workorders.BatchUploadResult;
import com.mirceone.inventoryapp.service.workorders.FileDownload;
import com.mirceone.inventoryapp.service.workorders.FileSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class)
class FileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderFileService fileService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    private final UUID userId = UUID.randomUUID();
    private final UUID firmId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();

    @BeforeEach
    void rateLimitAllows() {
        when(documentUploadRateLimiter.allow(any())).thenReturn(true);
        when(authRateLimiter.allow(any())).thenReturn(true);
    }

    @Test
    void uploadReturns201WithFolderAssignment() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.upload(eq(userId), eq(firmId), eq(workOrderId), any()))
                .thenReturn(sampleSummary(fileId, "photo.png", "Poze"));

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2});

        mockMvc.perform(multipart("/firms/{firmId}/work-orders/{workOrderId}/files", firmId, workOrderId)
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(fileId.toString()))
                .andExpect(jsonPath("$.folderPath").value("Poze"))
                .andExpect(jsonPath("$.folderId").value(folderId.toString()));
    }

    @Test
    void batchUploadReturnsPerFileResults() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.uploadBatch(eq(userId), eq(firmId), eq(workOrderId), any()))
                .thenReturn(new BatchUploadResult(
                        List.of(new BatchUploadResult.BatchUploadItem(fileId, "a.png", folderId, "Poze")),
                        List.of(new BatchUploadResult.BatchUploadError("bad..name", "Invalid filename"))
                ));

        MockMultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1, 2});

        mockMvc.perform(multipart("/firms/{firmId}/work-orders/{workOrderId}/files/batch", firmId, workOrderId)
                        .file(f1)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted[0].folderPath").value("Poze"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid filename"));
    }

    @Test
    void listFilesReturnsPage() throws Exception {
        FileSummary row = sampleSummary(UUID.randomUUID(), "Note.txt", "Documents");
        when(fileService.listFiles(eq(userId), eq(firmId), eq(workOrderId), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/files", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].workOrderId").value(workOrderId.toString()))
                .andExpect(jsonPath("$.content[0].folderPath").value("Documents"));
    }

    @Test
    void downloadContentReturnsResource() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.openForDownload(eq(userId), eq(firmId), eq(workOrderId), eq(fileId)))
                .thenReturn(new FileDownload(
                        "hello.txt",
                        "text/plain",
                        new ByteArrayResource("body".getBytes(StandardCharsets.UTF_8))
                ));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/files/{fileId}/content", firmId, workOrderId, fileId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void patchFileRenameReturns200() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.updateFile(eq(userId), eq(firmId), eq(workOrderId), eq(fileId), any(WorkOrderContracts.UpdateFileSpec.class)))
                .thenReturn(sampleSummary(fileId, "renamed.txt", "Documents"));

        mockMvc.perform(patch("/firms/{firmId}/work-orders/{workOrderId}/files/{fileId}", firmId, workOrderId, fileId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"renamed.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("renamed.txt"));
    }

    @Test
    void deleteFileReturns204() throws Exception {
        UUID fileId = UUID.randomUUID();
        doNothing().when(fileService).deleteFile(eq(userId), eq(firmId), eq(workOrderId), eq(fileId));

        mockMvc.perform(delete("/firms/{firmId}/work-orders/{workOrderId}/files/{fileId}", firmId, workOrderId, fileId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void serviceForbiddenMapsTo403() throws Exception {
        when(fileService.listFiles(eq(userId), eq(firmId), eq(workOrderId), isNull(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no"));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/files", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isForbidden());
    }

    private FileSummary sampleSummary(UUID fileId, String name, String folderPath) {
        return new FileSummary(
                fileId,
                firmId,
                workOrderId,
                folderId,
                folderPath,
                name,
                "txt",
                "text/plain",
                4,
                Instant.parse("2026-01-01T12:00:00Z"),
                userId,
                "u@example.com"
        );
    }
}
