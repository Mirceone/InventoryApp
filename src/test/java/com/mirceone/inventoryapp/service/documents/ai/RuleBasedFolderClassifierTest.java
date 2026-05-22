package com.mirceone.inventoryapp.service.documents.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedFolderClassifierTest {

    private final RuleBasedFolderClassifier classifier = new RuleBasedFolderClassifier();

    @Test
    void classifiesPngAsPoze() {
        assertEquals(Optional.of("Poze"), classifier.classify("photo.png", "image/png"));
    }

    @Test
    void classifiesSkpAsDocumente() {
        assertEquals(Optional.of("Documente"), classifier.classify("model.skp", "application/octet-stream"));
    }

    @Test
    void classifiesDwgAsDocumente() {
        assertEquals(Optional.of("Documente"), classifier.classify("plan.dwg", "application/octet-stream"));
    }

    @Test
    void classifiesPdfAsDocumente() {
        assertEquals(Optional.of("Documente"), classifier.classify("spec.pdf", "application/pdf"));
    }

    @Test
    void classifiesInvoicePdfAsFacturi() {
        assertEquals(Optional.of("Facturi"), classifier.classify("factura_123.pdf", "application/pdf"));
    }

    @Test
    void classifiesRenderPngAsRenders() {
        assertEquals(Optional.of("Renders"), classifier.classify("final_render.png", "image/png"));
    }

    @Test
    void unknownExtensionReturnsEmpty() {
        assertTrue(classifier.classify("archive.zip", "application/zip").isEmpty());
    }
}
