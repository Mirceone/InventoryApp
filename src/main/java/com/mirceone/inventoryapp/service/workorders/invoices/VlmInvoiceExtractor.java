package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.service.ai.AiImage;
import com.mirceone.inventoryapp.service.ai.AiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Transcribes scanned (image-only) invoices to markdown using the local vision model. Image uploads
 * are sent directly; scanned PDFs are rasterized first. Replaces the former Tesseract OCR fallback.
 */
@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class VlmInvoiceExtractor {

    private static final String PROMPT = """
            Transcribe ALL text from this invoice image into clean Markdown.
            Preserve the line-item table with its columns (description, quantity, unit price, total).
            Output only the transcription, with no commentary.""";

    private final AiService aiService;
    private final PdfPageRenderer pageRenderer;
    private final AppIntegrationProperties props;

    public VlmInvoiceExtractor(
            AiService aiService,
            PdfPageRenderer pageRenderer,
            AppIntegrationProperties props
    ) {
        this.aiService = aiService;
        this.pageRenderer = pageRenderer;
        this.props = props;
    }

    public String extract(Path sourceFile, String mimeType) throws IOException {
        List<AiImage> images;
        if (mimeType != null && mimeType.startsWith("image/")) {
            images = List.of(new AiImage(mimeType, Files.readAllBytes(sourceFile)));
        } else {
            int maxPages = Math.max(1, props.getInvoices().getVlmMaxPages());
            int dpi = Math.max(72, props.getInvoices().getVlmRenderDpi());
            images = pageRenderer.renderToPng(sourceFile, maxPages, dpi).stream()
                    .map(png -> new AiImage("image/png", png))
                    .toList();
        }
        if (images.isEmpty()) {
            throw new IOException("No pages to transcribe from " + sourceFile.getFileName());
        }

        String markdown = aiService.chatVision(PROMPT, images);
        if (markdown == null || markdown.isBlank()) {
            throw new IOException("VLM produced empty output");
        }
        return markdown.strip();
    }
}
