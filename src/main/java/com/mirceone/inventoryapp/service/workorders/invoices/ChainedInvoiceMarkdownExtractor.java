package com.mirceone.inventoryapp.service.workorders.invoices;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Routes invoice extraction by document type:
 * <ul>
 *   <li>PDF with a real text layer → MarkItDown (fast, accurate on digital PDFs)</li>
 *   <li>scanned PDF (no text layer) or image upload → VLM transcription</li>
 *   <li>other document types → MarkItDown</li>
 * </ul>
 * The downstream LLM structuring step then runs regardless of which extractor produced the text.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class ChainedInvoiceMarkdownExtractor implements InvoiceMarkdownExtractor {

    private final MarkItDownCliExtractor markItDownExtractor;
    private final VlmInvoiceExtractor vlmExtractor;
    private final PdfTextLayerDetector textLayerDetector;

    public ChainedInvoiceMarkdownExtractor(
            MarkItDownCliExtractor markItDownExtractor,
            VlmInvoiceExtractor vlmExtractor,
            PdfTextLayerDetector textLayerDetector
    ) {
        this.markItDownExtractor = markItDownExtractor;
        this.vlmExtractor = vlmExtractor;
        this.textLayerDetector = textLayerDetector;
    }

    @Override
    public String extract(Path sourceFile, String mimeType) throws IOException {
        if (isImage(mimeType)) {
            return vlmExtractor.extract(sourceFile, mimeType);
        }
        if (isPdf(mimeType)) {
            if (!textLayerDetector.hasTextLayer(sourceFile)) {
                return vlmExtractor.extract(sourceFile, mimeType);
            }
            try {
                return markItDownExtractor.extract(sourceFile, mimeType);
            } catch (IOException markItDownError) {
                // Text layer detected but MarkItDown produced nothing usable: fall back to the VLM.
                if (isEmptyOutput(markItDownError)) {
                    return vlmExtractor.extract(sourceFile, mimeType);
                }
                throw markItDownError;
            }
        }
        // Office docs, etc.: MarkItDown handles these.
        return markItDownExtractor.extract(sourceFile, mimeType);
    }

    private static boolean isPdf(String mimeType) {
        return mimeType != null && mimeType.startsWith("application/pdf");
    }

    private static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private static boolean isEmptyOutput(IOException error) {
        String message = error.getMessage();
        return message != null && message.contains("empty output");
    }
}
