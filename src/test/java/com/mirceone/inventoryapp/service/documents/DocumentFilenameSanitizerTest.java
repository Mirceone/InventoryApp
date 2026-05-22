package com.mirceone.inventoryapp.service.documents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentFilenameSanitizerTest {

    @Test
    void stripsDirectoryComponents() {
        assertEquals("x.txt", DocumentFilenameSanitizer.sanitizeOriginalFilename("C:\\dir\\x.txt"));
        assertEquals("x.txt", DocumentFilenameSanitizer.sanitizeOriginalFilename("/a/b/x.txt"));
    }

    @Test
    void rejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () -> DocumentFilenameSanitizer.sanitizeOriginalFilename(".."));
        assertThrows(IllegalArgumentException.class, () -> DocumentFilenameSanitizer.sanitizeOriginalFilename("a..b.txt"));
    }

    @Test
    void replacesUnsafeCharacters() {
        assertEquals("a_b_c.txt", DocumentFilenameSanitizer.sanitizeOriginalFilename("a<b>c.txt"));
    }

    @Test
    void truncatesLongName() {
        String longName = "a".repeat(250) + ".txt";
        String result = DocumentFilenameSanitizer.sanitizeOriginalFilename(longName);
        assertEquals(200, result.length());
    }

    @Test
    void storageSuffixAcceptsSimpleExtension() {
        assertEquals(".pdf", DocumentFilenameSanitizer.storageFileSuffix("report.pdf"));
    }

    @Test
    void storageSuffixFallsBackToBinWhenExtensionInvalid() {
        assertEquals(".bin", DocumentFilenameSanitizer.storageFileSuffix("bad.<>txt"));
    }
}
