package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Tries MarkItDown first, then OCR for scanned PDFs/images when MarkItDown returns empty.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class ChainedInvoiceMarkdownExtractor implements InvoiceMarkdownExtractor {

    private final MarkItDownCliExtractor markItDownExtractor;
    private final ScannedInvoiceOcrCliExtractor ocrExtractor;
    private final AppIntegrationProperties props;

    public ChainedInvoiceMarkdownExtractor(
            MarkItDownCliExtractor markItDownExtractor,
            ScannedInvoiceOcrCliExtractor ocrExtractor,
            AppIntegrationProperties props
    ) {
        this.markItDownExtractor = markItDownExtractor;
        this.ocrExtractor = ocrExtractor;
        this.props = props;
    }

    @Override
    public String extract(Path sourceFile, String mimeType) throws IOException {
        try {
            return markItDownExtractor.extract(sourceFile, mimeType);
        } catch (IOException markItDownError) {
            if (!shouldUseOcrFallback(mimeType, markItDownError)) {
                throw markItDownError;
            }
            // #region agent log
            InvoiceDebugLog.write("F", "ChainedInvoiceMarkdownExtractor.extract",
                    "MarkItDown empty, falling back to OCR",
                    InvoiceDebugLog.data(
                            "mimeType", mimeType,
                            "file", sourceFile.getFileName().toString(),
                            "markItDownError", markItDownError.getMessage()));
            // #endregion
            return ocrExtractor.extract(sourceFile, mimeType);
        }
    }

    private boolean shouldUseOcrFallback(String mimeType, IOException error) {
        if (!props.getInvoices().isOcrFallbackEnabled()) {
            return false;
        }
        if (mimeType == null || !isOcrMimeType(mimeType)) {
            return false;
        }
        String message = error.getMessage();
        return message != null && message.contains("empty output");
    }

    private static boolean isOcrMimeType(String mimeType) {
        return mimeType.startsWith("application/pdf") || mimeType.startsWith("image/");
    }
}
