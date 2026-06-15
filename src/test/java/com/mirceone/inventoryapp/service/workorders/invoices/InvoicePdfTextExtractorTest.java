package com.mirceone.inventoryapp.service.workorders.invoices;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfTextExtractorTest {

    private final InvoicePdfTextExtractor extractor = new InvoicePdfTextExtractor();

    @Test
    void extractsTextLayerFromPdf() throws Exception {
        Path pdf = Files.createTempFile("invoice-text-", ".pdf");
        try {
            writeTextPdf(pdf, "Factura 123 - Widget A x2");
            String text = extractor.extract(pdf);
            assertTrue(text.contains("Factura 123"));
            assertTrue(text.contains("Widget A"));
        } finally {
            Files.deleteIfExists(pdf);
        }
    }

    @Test
    void throwsWhenPdfHasNoTextLayer() throws Exception {
        Path pdf = Files.createTempFile("invoice-empty-", ".pdf");
        try {
            try (PDDocument doc = new PDDocument()) {
                doc.addPage(new PDPage()); // blank page, no text
                doc.save(pdf.toFile());
            }
            assertThrows(IOException.class, () -> extractor.extract(pdf));
        } finally {
            Files.deleteIfExists(pdf);
        }
    }

    private static void writeTextPdf(Path target, String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(target.toFile());
        }
    }
}
