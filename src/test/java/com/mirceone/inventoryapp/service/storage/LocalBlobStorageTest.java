package com.mirceone.inventoryapp.service.storage;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalBlobStorageTest {

    @TempDir
    Path tempDir;

    private LocalBlobStorage storage;

    @BeforeEach
    void setUp() {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getStorage().setRoot(tempDir.toString());
        storage = new LocalBlobStorage(props);
    }

    @Test
    void storeAndOpenRoundTrip() throws Exception {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        long written = storage.store("firm/wo/file-1.pdf", new ByteArrayInputStream(body), body.length);

        assertEquals(body.length, written);
        assertArrayEquals(body, storage.open("firm/wo/file-1.pdf").getInputStream().readAllBytes());
    }

    @Test
    void storeRejectsSizeMismatch() {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class,
                () -> storage.store("firm/wo/file-2.pdf", new ByteArrayInputStream(body), body.length + 5));
        assertFalse(Files.exists(tempDir.resolve("firm/wo/file-2.pdf")));
    }

    @Test
    void deleteRemovesBlob() throws Exception {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        storage.store("firm/wo/file-3.bin", new ByteArrayInputStream(body), body.length);

        storage.delete("firm/wo/file-3.bin");

        assertFalse(Files.exists(tempDir.resolve("firm/wo/file-3.bin")));
    }

    @Test
    void deleteByPrefixRemovesWholeSubtree() throws Exception {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        storage.store("firm/wo-a/f1.bin", new ByteArrayInputStream(body), body.length);
        storage.store("firm/wo-a/f2.bin", new ByteArrayInputStream(body), body.length);
        storage.store("firm/wo-b/f3.bin", new ByteArrayInputStream(body), body.length);

        storage.deleteByPrefix("firm/wo-a/");

        assertFalse(Files.exists(tempDir.resolve("firm/wo-a")));
        assertTrue(Files.exists(tempDir.resolve("firm/wo-b/f3.bin")));
    }

    @Test
    void rejectsPathTraversalKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.open("../outside.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> storage.open("/absolute.txt"));
    }
}
