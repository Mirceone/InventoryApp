package com.mirceone.inventoryapp.service.workorders.invoices.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.InvoiceExtractionEntity;
import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;
import com.mirceone.inventoryapp.model.InvoiceLineItemEntity;
import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import com.mirceone.inventoryapp.repository.InvoiceExtractionRepository;
import com.mirceone.inventoryapp.repository.InvoiceLineItemRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.ai.AiModelIdResolver;
import com.mirceone.inventoryapp.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns an invoice's extracted markdown into structured line items via the local LLM
 * ({@link AiService#chatJson}). Phase 1 of invoice-driven inventory: extraction only — matching
 * against firm products and applying to inventory are handled by later phases.
 *
 * <p>Mirrors the existing async pipeline: a PENDING {@link InvoiceExtractionEntity} row is created
 * when an invoice becomes READY, then processed here via FOR UPDATE SKIP LOCKED so the per-invoice
 * trigger and the scheduled poller never double-process.
 */
@Service
public class InvoiceStructuringService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceStructuringService.class);

    /** Cap the markdown fed to the model so a huge document cannot blow the context window. */
    private static final int MAX_MARKDOWN_CHARS = 24_000;
    /** Marker the stub AI keys on; also makes the intent explicit to the model. */
    static final String PROMPT_MARKER = "Extract the structured data from this invoice";

    private final InvoiceExtractionRepository extractionRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final WorkOrderInvoiceRepository invoiceRepository;
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
            AiService aiService,
            ObjectProvider<AiModelIdResolver> modelIdResolver,
            AppIntegrationProperties props,
            ObjectMapper objectMapper,
            @Lazy InvoiceStructuringService self
    ) {
        this.extractionRepository = extractionRepository;
        this.lineItemRepository = lineItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.aiService = aiService;
        this.modelIdResolver = modelIdResolver;
        this.props = props;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Async
    public void processAsync(UUID extractionId) {
        try {
            // Through the proxy so @Transactional applies on the async thread (a direct call would
            // be self-invocation and skip the transaction).
            self.processExtraction(extractionId);
        } catch (Exception e) {
            log.warn("Async invoice structuring failed id={}: {}", extractionId, e.getMessage());
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
                log.warn("Invoice structuring failed id={}: {}", extraction.getId(), e.getMessage());
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
        String markdown = invoice != null ? invoice.getMarkdownText() : null;
        if (markdown == null || markdown.isBlank()) {
            markFailed(extraction, "Invoice has no extracted text to structure");
            return;
        }

        try {
            String raw = aiService.chatJson(buildPrompt(markdown));
            ExtractedInvoiceJson parsed = objectMapper.readValue(raw, ExtractedInvoiceJson.class);
            applyExtraction(extraction, parsed);
            extraction.setStatus(InvoiceExtractionStatus.READY);
            extraction.setError(null);
            extraction.setModel(resolveModel());
            extraction.setExtractedAt(Instant.now());
            extractionRepository.save(extraction);
        } catch (Exception e) {
            // Persist the failure in this transaction without rethrowing (a rethrow would roll the
            // FAILED write back through the @Transactional boundary).
            log.warn("Invoice structuring failed id={}: {}", extraction.getId(), e.getMessage());
            markFailed(extraction, e.getMessage());
        }
    }

    private void applyExtraction(InvoiceExtractionEntity extraction, ExtractedInvoiceJson parsed) {
        // Idempotent re-run: clear any previously extracted lines before persisting fresh ones.
        lineItemRepository.deleteByExtractionId(extraction.getId());

        extraction.setSupplierName(InvoiceValueParser.trimToNull(parsed.supplier(), 512));
        extraction.setInvoiceNumber(InvoiceValueParser.trimToNull(parsed.invoiceNumber(), 128));
        extraction.setInvoiceDate(InvoiceValueParser.date(parsed.invoiceDate()));
        extraction.setCurrency(InvoiceValueParser.trimToNull(parsed.currency(), 8));
        extraction.setTotalAmount(InvoiceValueParser.money(parsed.total()));

        List<ExtractedInvoiceJson.Line> lines = parsed.lineItems() != null ? parsed.lineItems() : List.of();
        List<InvoiceLineItemEntity> rows = new ArrayList<>();
        int lineNo = 1;
        for (ExtractedInvoiceJson.Line line : lines) {
            String description = InvoiceValueParser.trimToNull(line.description(), Integer.MAX_VALUE);
            if (description == null) {
                continue; // a line with no description carries no inventory signal
            }
            rows.add(new InvoiceLineItemEntity(
                    UUID.randomUUID(),
                    extraction.getId(),
                    extraction.getFirmId(),
                    lineNo++,
                    description,
                    InvoiceValueParser.trimToNull(line.sku(), 128),
                    InvoiceValueParser.money(line.quantity()),
                    InvoiceValueParser.trimToNull(line.unit(), 32),
                    InvoiceValueParser.money(line.unitPrice()),
                    InvoiceValueParser.money(line.lineTotal())
            ));
        }
        lineItemRepository.saveAll(rows);
    }

    private void markFailed(InvoiceExtractionEntity extraction, String message) {
        extraction.setStatus(InvoiceExtractionStatus.FAILED);
        extraction.setError(truncate(message, 2000));
        extraction.setModel(resolveModel());
        extraction.setExtractedAt(Instant.now());
        extractionRepository.save(extraction);
    }

    private String buildPrompt(String markdown) {
        String text = markdown.length() > MAX_MARKDOWN_CHARS ? markdown.substring(0, MAX_MARKDOWN_CHARS) : markdown;
        return PROMPT_MARKER + ". Reply with JSON only, no prose.\n"
                + "Schema: {\"supplier\": string|null, \"invoiceNumber\": string|null, "
                + "\"invoiceDate\": \"YYYY-MM-DD\"|null, \"currency\": string|null, \"total\": number|null, "
                + "\"lineItems\": [{\"description\": string, \"sku\": string|null, \"quantity\": number|null, "
                + "\"unit\": string|null, \"unitPrice\": number|null, \"lineTotal\": number|null}]}\n"
                + "Only include products or services actually listed. Do not invent values; use null when unknown.\n\n"
                + "Invoice:\n" + text;
    }

    private String resolveModel() {
        AiModelIdResolver resolver = modelIdResolver.getIfAvailable();
        return resolver != null ? resolver.resolvedModelId() : props.getAi().getModel();
    }

    private static String truncate(String message, int max) {
        if (message == null) {
            return null;
        }
        return message.length() <= max ? message : message.substring(0, max);
    }
}
