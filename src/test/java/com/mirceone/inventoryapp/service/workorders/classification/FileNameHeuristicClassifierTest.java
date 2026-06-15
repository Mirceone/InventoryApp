package com.mirceone.inventoryapp.service.workorders.classification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNameHeuristicClassifierTest {

    private FileNameHeuristicClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new FileNameHeuristicClassifier();
    }

    @Test
    void invoiceFilenameMapsToFacturi() {
        assertEquals("Facturi", classifier.hint("factura_123.pdf", "application/pdf").orElseThrow());
    }

    @Test
    void renderImageMapsToRenders() {
        assertEquals("Renders", classifier.hint("kitchen_render.jpg", "image/jpeg").orElseThrow());
    }

    @Test
    void plainImageMapsToPoze() {
        assertEquals("Poze", classifier.hint("site.png", "image/png").orElseThrow());
    }

    @Test
    void pdfMapsToDocuments() {
        assertEquals("Documents", classifier.hint("spec.pdf", "application/pdf").orElseThrow());
    }

    @Test
    void unknownFileReturnsEmpty() {
        assertTrue(classifier.hint("notes.xyz", "application/octet-stream").isEmpty());
    }

    @Test
    void canonicalizesSynonyms() {
        assertEquals("Facturi", classifier.canonicalFolderName("invoice").orElseThrow());
        assertEquals("Documents", classifier.canonicalFolderName("documente").orElseThrow());
    }
}
