package com.mirceone.inventoryapp.service.workorders.invoices.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.InvoiceExtractionEntity;
import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;
import com.mirceone.inventoryapp.model.InvoiceLineItemEntity;
import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import com.mirceone.inventoryapp.repository.InvoiceExtractionRepository;
import com.mirceone.inventoryapp.repository.InvoiceLineItemRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.ai.AiImage;
import com.mirceone.inventoryapp.service.ai.AiModelIdResolver;
import com.mirceone.inventoryapp.service.ai.AiService;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.workorders.invoices.InvoicePdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Extracts product candidates from an invoice into structured JSON.
 *
 * <p>Text PDFs (the common case) are read with PDFBox and sent to the LLM as text — accurate and
 * cheap. Image uploads fall back to the vision model. Either way the model returns product-shaped
 * JSON which is parsed and persisted. Calls are serialized by the AI inference gate.
 */
@Service
public class InvoiceStructuringService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceStructuringService.class);

    private static final int MAX_TEXT_CHARS = 24_000;

    /** Marker the stub keys on; also states the task to the model. */
    static final String TEXT_PROMPT_HEADER = """
            You are an invoice parser. Extract all line items and return ONLY a JSON object,
            with no explanation outside the JSON.
            Extract products from this invoice.
            Return exactly: {"products":[{"name":"...","sku":null,"quantity":1.0}]}
            Quantities must be numbers (not strings). SKU is null if not present.

            Invoice text:
            """;

    private static final String VISION_PROMPT = """
            Read this invoice image and extract the products to add to inventory.
            Reply with JSON only, no prose and no markdown fences.
            Schema: {"products":[{"name": string, "sku": string|null, "quantity": number}]}
            Include one entry per distinct product line. Do not invent products.""";

    private final InvoiceExtractionRepository extractionRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final WorkOrderInvoiceRepository invoiceRepository;
    private final BlobStorage blobStorage;
    private final InvoicePdfTextExtractor pdfTextExtractor;
    private final AiService aiService;
    private final ObjectProvider<AiModelIdResolver> modelIdResolver;
    private final AppIntegrationProperties props;
    private final ObjectMapper objectMapper;
    /** Self-reference so the async path calls processExtraction through the transactional proxy. */
    private final InvoiceStructuringService self;

    public InvoiceStructuringService(
            InvoiceExtractionRepository extractionRepository,
            InvoiceLineItemRepository lineItemRepository,
            WorkOrderInvoiceRepository invoiceRepository,
            BlobStorage blobStorage,
            InvoicePdfTextExtractor pdfTextExtractor,
            @Qualifier("localAi") AiService aiService,
            ObjectProvider<AiModelIdResolver> modelIdResolver,
            AppIntegrationProperties props,
            ObjectMapper objectMapper,
            @Lazy InvoiceStructuringService self
    ) {
        this.extractionRepository = extractionRepository;
        this.lineItemRepository = lineItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.blobStorage = blobStorage;
        this.pdfTextExtractor = pdfTextExtractor;
        this.aiService = aiService;
        this.modelIdResolver = modelIdResolver;
        this.props = props;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Async
    public void processAsync(UUID extractionId) {
        try {
            self.processExtraction(extractionId);
        } catch (Exception e) {
            log.warn("Async invoice extraction failed id={}: {}", extractionId, e.getMessage());
        }
    }

    @Transactional
    public int processPendingBatch(int batchSize) {
        List<InvoiceExtractionEntity> batch = extractionRepository.lockNextPendingBatch(batchSize);
        int processed = 0;
        for (InvoiceExtractionEntity extraction : batch) {
            try {
                processLocked(extraction);
                processed++;
            } catch (Exception e) {
                log.warn("Invoice extraction failed id={}: {}", extraction.getId(), e.getMessage());
            }
        }
        return processed;
    }

    @Transactional
    public void processExtraction(UUID extractionId) {
        InvoiceExtractionEntity extraction = extractionRepository.lockPendingById(extractionId).orElse(null);
        if (extraction == null) {
            return;
        }
        processLocked(extraction);
    }

    private void processLocked(InvoiceExtractionEntity extraction) {
        WorkOrderInvoiceEntity invoice = invoiceRepository.findById(extraction.getInvoiceId()).orElse(null);
        if (invoice == null) {
            markFailed(extraction, null, "Invoice not found for extraction");
            return;
        }

        Path tempFile = null;
        try {
            tempFile = materializeBlob(invoice);
            String raw = callModel(tempFile, invoice.getMimeType());
            applyExtraction(extraction, raw);
            extraction.setStatus(InvoiceExtractionStatus.READY);
            extraction.setError(null);
            extraction.setModel(resolveModel());
            extraction.setExtractedAt(Instant.now());
            extractionRepository.save(extraction);
            mirrorInvoiceStatus(invoice, InvoiceProcessingStatus.READY);
        } catch (Exception e) {
            log.warn("Invoice extraction failed id={}: {}", extraction.getId(), e.getMessage());
            markFailed(extraction, invoice, e.getMessage());
        } finally {
            deleteQuietly(tempFile);
        }
    }

    /** Text PDFs go through PDFBox + text LLM; image uploads use the vision model. */
    private String callModel(Path file, String mimeType) throws IOException {
        if (mimeType != null && mimeType.startsWith("image/")) {
            AiImage image = new AiImage(mimeType, Files.readAllBytes(file));
            return aiService.chatVision(VISION_PROMPT, List.of(image));
        }
        String text = pdfTextExtractor.extract(file);
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }
        return aiService.chatJson(TEXT_PROMPT_HEADER + text);
    }

    private void applyExtraction(InvoiceExtractionEntity extraction, String rawResponse) throws IOException {
        String json = stripToJson(rawResponse);
        ExtractedInvoiceJson parsed = objectMapper.readValue(json, ExtractedInvoiceJson.class);

        lineItemRepository.deleteByExtractionId(extraction.getId()); // idempotent re-run
        extraction.setRawJson(truncate(rawResponse, 100_000));

        List<ExtractedInvoiceJson.Product> products = parsed.products() != null ? parsed.products() : List.of();
        List<InvoiceLineItemEntity> rows = new ArrayList<>();
        int lineNo = 1;
        for (ExtractedInvoiceJson.Product product : products) {
            String name = InvoiceValueParser.trimToNull(product.name(), 1000);
            if (name == null) {
                continue; // no product name → nothing to add to inventory
            }
            BigDecimal quantity = InvoiceValueParser.money(product.quantity());
            if (quantity != null && quantity.signum() <= 0) {
                quantity = null; // ignore zero/negative quantities rather than fabricating one
            }
            rows.add(new InvoiceLineItemEntity(
                    UUID.randomUUID(),
                    extraction.getId(),
                    extraction.getFirmId(),
                    lineNo++,
                    name,
                    InvoiceValueParser.trimToNull(product.sku(), 128),
                    quantity
            ));
        }
        lineItemRepository.saveAll(rows);
    }

    private Path materializeBlob(WorkOrderInvoiceEntity invoice) throws IOException {
        Resource resource = blobStorage.open(invoice.getStorageKey());
        String suffix = invoice.getExtension() != null && !invoice.getExtension().isBlank()
                ? "." + invoice.getExtension()
                : ".bin";
        Path tempFile = Files.createTempFile("invoice-extract-" + invoice.getId(), suffix);
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private void markFailed(InvoiceExtractionEntity extraction, WorkOrderInvoiceEntity invoice, String message) {
        extraction.setStatus(InvoiceExtractionStatus.FAILED);
        extraction.setError(truncate(message, 2000));
        extraction.setModel(resolveModel());
        extraction.setExtractedAt(Instant.now());
        extractionRepository.save(extraction);
        if (invoice != null) {
            mirrorInvoiceStatus(invoice, InvoiceProcessingStatus.FAILED);
        }
    }

    private void mirrorInvoiceStatus(WorkOrderInvoiceEntity invoice, InvoiceProcessingStatus status) {
        invoice.setProcessingStatus(status);
        invoice.setProcessedAt(Instant.now());
        invoiceRepository.save(invoice);
    }

    private String resolveModel() {
        AiModelIdResolver resolver = modelIdResolver.getIfAvailable();
        return resolver != null ? resolver.resolvedModelId() : props.getAi().getModel();
    }

    /** Strips markdown code fences / surrounding prose so a tolerant JSON object can be parsed. */
    static String stripToJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) {
                s = s.substring(nl + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.strip();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    private static String truncate(String message, int max) {
        if (message == null) {
            return null;
        }
        return message.length() <= max ? message : message.substring(0, max);
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
