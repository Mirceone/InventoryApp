package com.mirceone.inventoryapp.service.workorders.invoices;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Converts an invoice file on disk to markdown. v1 uses MarkItDown CLI; a future AI implementation
 * can replace this without changing the processing pipeline.
 */
public interface InvoiceMarkdownExtractor {

    String extract(Path sourceFile, String mimeType) throws IOException;
}
