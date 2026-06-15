package com.mirceone.inventoryapp.service.workorders.invoices;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Extracts the native text layer from a PDF via PDFBox. Throws when the PDF is encrypted or has no
 * usable text (e.g. a pure scan) so the caller can fail the extraction or fall back to vision.
 */
@Component
public class InvoicePdfTextExtractor {

    public String extract(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            if (document.isEncrypted()) {
                throw new IOException("PDF is encrypted and cannot be read");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IOException("PDF has no extractable text layer");
            }
            return text.strip();
        }
    }
}
