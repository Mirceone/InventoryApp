package com.mirceone.inventoryapp.service.workorders.invoices;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Decides whether a PDF carries a real (selectable) text layer. PDFs with text go to MarkItDown;
 * scanned/image-only PDFs are routed to the VLM for transcription.
 */
@Component
public class PdfTextLayerDetector {

    private static final Logger log = LoggerFactory.getLogger(PdfTextLayerDetector.class);

    /** Below this many non-whitespace characters we treat the PDF as scanned (no usable text). */
    private static final int MIN_TEXT_CHARS = 20;
    /** Only inspect the first few pages; enough to tell text from scan, and fast. */
    private static final int PROBE_PAGES = 5;

    public boolean hasTextLayer(Path pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(document.getNumberOfPages(), PROBE_PAGES));
            String text = stripper.getText(document);
            long nonWhitespace = text.chars().filter(c -> !Character.isWhitespace(c)).count();
            return nonWhitespace >= MIN_TEXT_CHARS;
        } catch (IOException | RuntimeException e) {
            // Unreadable text layer → treat as scanned so the VLM path handles it.
            log.debug("PDF text-layer probe failed for {}: {}", pdf.getFileName(), e.getMessage());
            return false;
        }
    }
}
