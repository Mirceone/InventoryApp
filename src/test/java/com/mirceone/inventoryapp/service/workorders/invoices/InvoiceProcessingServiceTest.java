package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceProcessingServiceTest {

    @Mock
    private WorkOrderInvoiceRepository invoiceRepository;
    @Mock
    private BlobStorage blobStorage;
    @Mock
    private InvoiceMarkdownExtractor markdownExtractor;

    private InvoiceProcessingService processingService;

    @BeforeEach
    void setUp() {
        processingService = new InvoiceProcessingService(invoiceRepository, blobStorage, markdownExtractor);
    }

    @Test
    void processPendingBatchMarksReadyWhenExtractorSucceeds() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        WorkOrderInvoiceEntity invoice = sampleInvoice(invoiceId);
        when(invoiceRepository.lockNextPendingBatch(5)).thenReturn(List.of(invoice));
        when(blobStorage.open(invoice.getStorageKey()))
                .thenReturn(new ByteArrayResource("pdf".getBytes(StandardCharsets.UTF_8)));
        when(markdownExtractor.extract(any(), eq("application/pdf"))).thenReturn("# Invoice");

        int processed = processingService.processPendingBatch(5);

        assertEquals(1, processed);
        ArgumentCaptor<WorkOrderInvoiceEntity> captor = ArgumentCaptor.forClass(WorkOrderInvoiceEntity.class);
        verify(invoiceRepository).saveAndFlush(captor.capture());
        WorkOrderInvoiceEntity saved = captor.getValue();
        assertEquals(InvoiceProcessingStatus.READY, saved.getProcessingStatus());
        assertEquals("# Invoice", saved.getMarkdownText());
        assertNull(saved.getProcessingError());
        assertNotNull(saved.getProcessedAt());
    }

    @Test
    void processInvoiceMarksFailedWhenExtractorThrows() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        WorkOrderInvoiceEntity invoice = sampleInvoice(invoiceId);
        when(invoiceRepository.lockPendingById(invoiceId)).thenReturn(Optional.of(invoice));
        when(blobStorage.open(invoice.getStorageKey()))
                .thenReturn(new ByteArrayResource("pdf".getBytes(StandardCharsets.UTF_8)));
        when(markdownExtractor.extract(any(), eq("application/pdf")))
                .thenThrow(new java.io.IOException("MarkItDown failed"));

        processingService.processInvoice(invoiceId);

        ArgumentCaptor<WorkOrderInvoiceEntity> captor = ArgumentCaptor.forClass(WorkOrderInvoiceEntity.class);
        verify(invoiceRepository).saveAndFlush(captor.capture());
        WorkOrderInvoiceEntity saved = captor.getValue();
        assertEquals(InvoiceProcessingStatus.FAILED, saved.getProcessingStatus());
        assertNull(saved.getMarkdownText());
        assertEquals("MarkItDown failed", saved.getProcessingError());
    }

    private static WorkOrderInvoiceEntity sampleInvoice(UUID invoiceId) {
        return new WorkOrderInvoiceEntity(
                invoiceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "invoice.pdf",
                "pdf",
                "application/pdf",
                3,
                "checksum",
                "firm/wo/invoices/" + invoiceId + ".pdf"
        );
    }
}
