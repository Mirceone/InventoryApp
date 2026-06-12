package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.InvoiceExtractionEntity;
import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import com.mirceone.inventoryapp.repository.InvoiceExtractionRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import com.mirceone.inventoryapp.service.workorders.invoices.extraction.InvoiceStructuringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceProcessingService.class);

    private final WorkOrderInvoiceRepository invoiceRepository;
    private final BlobStorage blobStorage;
    private final InvoiceMarkdownExtractor markdownExtractor;
    private final InvoiceExtractionRepository extractionRepository;
    private final InvoiceStructuringService structuringService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final AppIntegrationProperties props;

    public InvoiceProcessingService(
            WorkOrderInvoiceRepository invoiceRepository,
            BlobStorage blobStorage,
            InvoiceMarkdownExtractor markdownExtractor,
            InvoiceExtractionRepository extractionRepository,
            InvoiceStructuringService structuringService,
            AfterCommitExecutor afterCommitExecutor,
            AppIntegrationProperties props
    ) {
        this.invoiceRepository = invoiceRepository;
        this.blobStorage = blobStorage;
        this.markdownExtractor = markdownExtractor;
        this.extractionRepository = extractionRepository;
        this.structuringService = structuringService;
        this.afterCommitExecutor = afterCommitExecutor;
        this.props = props;
    }

    @Async
    public void processAsync(UUID invoiceId) {
        try {
            processInvoice(invoiceId);
        } catch (Exception e) {
            log.warn("Async invoice processing failed id={}: {}", invoiceId, e.getMessage());
        }
    }

    @Transactional
    public int processPendingBatch(int batchSize) {
        List<WorkOrderInvoiceEntity> batch = invoiceRepository.lockNextPendingBatch(batchSize);
        int processed = 0;
        for (WorkOrderInvoiceEntity invoice : batch) {
            try {
                processLockedInvoice(invoice);
                processed++;
            } catch (Exception e) {
                log.warn("Invoice processing failed id={}: {}", invoice.getId(), e.getMessage());
            }
        }
        return processed;
    }

    @Transactional
    public void processInvoice(UUID invoiceId) {
        WorkOrderInvoiceEntity invoice = invoiceRepository.lockPendingById(invoiceId).orElse(null);
        if (invoice == null) {
            return;
        }
        processLockedInvoice(invoice);
    }

    private void processLockedInvoice(WorkOrderInvoiceEntity invoice) {
        Path tempFile = null;
        try {
            tempFile = materializeBlob(invoice);
            String markdown = markdownExtractor.extract(tempFile, invoice.getMimeType());
            markReady(invoice, markdown);
        } catch (Exception e) {
            markFailed(invoice, e);
        } finally {
            if (tempFile != null) {
                deleteQuietly(tempFile);
            }
        }
    }

    private Path materializeBlob(WorkOrderInvoiceEntity invoice) throws IOException {
        Resource resource = blobStorage.open(invoice.getStorageKey());
        String suffix = invoice.getExtension() != null && !invoice.getExtension().isBlank()
                ? "." + invoice.getExtension()
                : ".bin";
        Path tempFile = Files.createTempFile("invoice-" + invoice.getId(), suffix);
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private void markReady(WorkOrderInvoiceEntity invoice, String markdown) {
        invoice.setProcessingStatus(InvoiceProcessingStatus.READY);
        invoice.setMarkdownText(markdown);
        invoice.setProcessingError(null);
        invoice.setProcessedAt(Instant.now());
        invoiceRepository.saveAndFlush(invoice);
        enqueueStructuring(invoice);
    }

    /**
     * Once an invoice has readable text, queue it for structured extraction (PENDING row picked up
     * by the structuring worker / async trigger). Best-effort: a failure here must not fail the
     * markdown extraction that already succeeded.
     */
    private void enqueueStructuring(WorkOrderInvoiceEntity invoice) {
        if (!props.getFeatures().isInvoiceExtractionEnabled()) {
            return;
        }
        if (extractionRepository.existsByInvoiceId(invoice.getId())) {
            return;
        }
        try {
            InvoiceExtractionEntity extraction = new InvoiceExtractionEntity(
                    UUID.randomUUID(), invoice.getId(), invoice.getFirmId(), invoice.getWorkOrderId());
            extractionRepository.save(extraction);
            UUID extractionId = extraction.getId();
            afterCommitExecutor.execute(() -> structuringService.processAsync(extractionId));
        } catch (Exception e) {
            log.warn("Failed to enqueue invoice structuring for invoice={}: {}", invoice.getId(), e.getMessage());
        }
    }

    private void markFailed(WorkOrderInvoiceEntity invoice, Exception error) {
        invoice.setProcessingStatus(InvoiceProcessingStatus.FAILED);
        invoice.setMarkdownText(null);
        invoice.setProcessingError(truncateError(error));
        invoice.setProcessedAt(Instant.now());
        invoiceRepository.saveAndFlush(invoice);
    }

    private static String truncateError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
