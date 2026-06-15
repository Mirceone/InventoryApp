package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.workorders.BatchInvoiceUploadResult;
import com.mirceone.inventoryapp.service.workorders.FileDownload;
import com.mirceone.inventoryapp.service.workorders.InvoiceSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderInvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvoiceController.class)
class InvoiceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderInvoiceService invoiceService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    private final UUID userId = UUID.randomUUID();
    private final UUID firmId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();

    @BeforeEach
    void rateLimitAllows() {
        when(documentUploadRateLimiter.allow(any())).thenReturn(true);
        when(authRateLimiter.allow(any())).thenReturn(true);
    }

    @Test
    void uploadReturns201WithPendingStatus() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.upload(eq(userId), eq(firmId), eq(workOrderId), any()))
                .thenReturn(sampleSummary(invoiceId, InvoiceProcessingStatus.PENDING, null));

        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/firms/{firmId}/work-orders/{workOrderId}/invoices", firmId, workOrderId)
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.processingStatus").value("PENDING"));
    }

    @Test
    void getInvoiceReturnsMarkdownWhenReady() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.getInvoice(userId, firmId, workOrderId, invoiceId))
                .thenReturn(sampleSummary(invoiceId, InvoiceProcessingStatus.READY, "# Invoice"));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/invoices/{invoiceId}",
                        firmId, workOrderId, invoiceId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("READY"))
                .andExpect(jsonPath("$.markdownText").value("# Invoice"));
    }

    @Test
    void listInvoicesReturnsPage() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.listInvoices(eq(userId), eq(firmId), eq(workOrderId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleSummary(invoiceId, InvoiceProcessingStatus.PENDING, null))));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/invoices", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(invoiceId.toString()));
    }

    @Test
    void batchUploadReturnsAcceptedItems() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.uploadBatch(eq(userId), eq(firmId), eq(workOrderId), any()))
                .thenReturn(new BatchInvoiceUploadResult(
                        List.of(new BatchInvoiceUploadResult.BatchInvoiceUploadItem(
                                invoiceId, "invoice.pdf", InvoiceProcessingStatus.PENDING)),
                        List.of()));

        MockMultipartFile file = new MockMultipartFile("files", "invoice.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/firms/{firmId}/work-orders/{workOrderId}/invoices/batch", firmId, workOrderId)
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted[0].id").value(invoiceId.toString()));
    }

    @Test
    void downloadContentReturnsBytes() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.openForDownload(userId, firmId, workOrderId, invoiceId))
                .thenReturn(new FileDownload(
                        "invoice.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        new ByteArrayResource(new byte[]{9, 8, 7})));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/invoices/{invoiceId}/content",
                        firmId, workOrderId, invoiceId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteInvoiceReturns204() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        doNothing().when(invoiceService).deleteInvoice(userId, firmId, workOrderId, invoiceId);

        mockMvc.perform(delete("/firms/{firmId}/work-orders/{workOrderId}/invoices/{invoiceId}",
                        firmId, workOrderId, invoiceId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void retryProcessingReturnsPending() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.retryProcessing(userId, firmId, workOrderId, invoiceId))
                .thenReturn(sampleSummary(invoiceId, InvoiceProcessingStatus.PENDING, null));

        mockMvc.perform(post("/firms/{firmId}/work-orders/{workOrderId}/invoices/{invoiceId}/retry",
                        firmId, workOrderId, invoiceId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("PENDING"));
    }

    private InvoiceSummary sampleSummary(UUID invoiceId, InvoiceProcessingStatus status, String markdown) {
        return new InvoiceSummary(
                invoiceId,
                workOrderId,
                "invoice.pdf",
                "pdf",
                "application/pdf",
                10,
                status,
                markdown,
                status == InvoiceProcessingStatus.FAILED ? "error" : null,
                Instant.parse("2026-01-01T00:00:00Z"),
                status == InvoiceProcessingStatus.PENDING ? null : Instant.parse("2026-01-01T00:00:05Z"),
                userId,
                "user@example.com"
        );
    }
}
