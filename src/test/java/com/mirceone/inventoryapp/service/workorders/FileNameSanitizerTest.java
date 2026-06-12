package com.mirceone.inventoryapp.service.workorders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileNameSanitizerTest {

    @Test
    void stripsDirectoryComponents() {
        assertEquals("x.txt", FileNameSanitizer.sanitizeDisplayName("C:\\dir\\x.txt"));
        assertEquals("x.txt", FileNameSanitizer.sanitizeDisplayName("/a/b/x.txt"));
    }

    @Test
    void rejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () -> FileNameSanitizer.sanitizeDisplayName(".."));
        assertThrows(IllegalArgumentException.class, () -> FileNameSanitizer.sanitizeDisplayName("a..b.txt"));
    }

    @Test
    void replacesUnsafeCharacters() {
        assertEquals("a_b_c.txt", FileNameSanitizer.sanitizeDisplayName("a<b>c.txt"));
    }

    @Test
    void truncatesLongName() {
        String longName = "a".repeat(250) + ".txt";
        String result = FileNameSanitizer.sanitizeDisplayName(longName);
        assertEquals(200, result.length());
    }

    @Test
    void storageSuffixAcceptsSimpleExtension() {
        assertEquals(".pdf", FileNameSanitizer.storageFileSuffix("report.pdf"));
    }

    @Test
    void storageSuffixFallsBackToBinWhenExtensionInvalid() {
        assertEquals(".bin", FileNameSanitizer.storageFileSuffix("bad.<>txt"));
        assertEquals(".bin", FileNameSanitizer.storageFileSuffix("noextension"));
    }
}
