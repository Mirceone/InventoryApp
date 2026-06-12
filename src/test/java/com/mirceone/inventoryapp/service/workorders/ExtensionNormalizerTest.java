package com.mirceone.inventoryapp.service.workorders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtensionNormalizerTest {

    @Test
    void normalizesRuleExtension() {
        assertEquals("pdf", ExtensionNormalizer.normalizeRuleExtension("pdf"));
        assertEquals("pdf", ExtensionNormalizer.normalizeRuleExtension(".PDF"));
        assertEquals("dwg", ExtensionNormalizer.normalizeRuleExtension("  dwg  "));
    }

    @Test
    void rejectsInvalidRuleExtension() {
        assertThrows(IllegalArgumentException.class, () -> ExtensionNormalizer.normalizeRuleExtension(""));
        assertThrows(IllegalArgumentException.class, () -> ExtensionNormalizer.normalizeRuleExtension("p df"));
        assertThrows(IllegalArgumentException.class, () -> ExtensionNormalizer.normalizeRuleExtension("a.b"));
        assertThrows(IllegalArgumentException.class, () -> ExtensionNormalizer.normalizeRuleExtension("x".repeat(20)));
    }

    @Test
    void extractsExtensionFromFilename() {
        assertEquals("pdf", ExtensionNormalizer.fromFilename("Report.PDF"));
        assertEquals("png", ExtensionNormalizer.fromFilename("photo.png"));
        assertEquals("", ExtensionNormalizer.fromFilename("noextension"));
        assertEquals("", ExtensionNormalizer.fromFilename("trailingdot."));
        assertEquals("", ExtensionNormalizer.fromFilename("weird.<>x"));
    }
}
