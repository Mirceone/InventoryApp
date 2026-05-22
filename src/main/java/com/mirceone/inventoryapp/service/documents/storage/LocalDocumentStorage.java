package com.mirceone.inventoryapp.service.documents.storage;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final AppIntegrationProperties props;

    public LocalDocumentStorage(AppIntegrationProperties props) {
        this.props = props;
    }

    @Override
    public long store(String storageKey, InputStream content, long expectedSize) throws IOException {
        Path target = resolveUnderRoot(storageKey);
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target)) {
            long written = content.transferTo(out);
            if (expectedSize >= 0 && written != expectedSize) {
                Files.deleteIfExists(target);
                throw new IOException("Size mismatch: expected " + expectedSize + " bytes, wrote " + written);
            }
            return written;
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path target = resolveUnderRoot(storageKey);
        Files.deleteIfExists(target);
        Path parent = target.getParent();
        if (parent != null) {
            try {
                Files.deleteIfExists(parent);
            } catch (IOException ignored) {
                // directory not empty or still in use — ignore
            }
        }
    }

    @Override
    public void move(String fromKey, String toKey) throws IOException {
        Path source = resolveUnderRoot(fromKey);
        Path target = resolveUnderRoot(toKey);
        if (!Files.isRegularFile(source)) {
            throw new java.io.FileNotFoundException("No file for key: " + fromKey);
        }
        Files.createDirectories(target.getParent());
        Files.move(source, target);
        Path parent = source.getParent();
        if (parent != null) {
            try {
                Files.deleteIfExists(parent);
            } catch (IOException ignored) {
                // not empty
            }
        }
    }

    @Override
    public Resource asResource(String storageKey) throws IOException {
        Path target = resolveUnderRoot(storageKey);
        if (!Files.isRegularFile(target)) {
            throw new java.io.FileNotFoundException("No file for key: " + storageKey);
        }
        return new FileSystemResource(target.toFile());
    }

    private Path resolveUnderRoot(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storage key required");
        }
        if (storageKey.contains("..") || storageKey.startsWith("/") || storageKey.startsWith("\\")) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        Path root = Path.of(props.getStorage().getRoot()).toAbsolutePath().normalize();
        Path relative = Path.of(storageKey.replace('/', java.io.File.separatorChar)).normalize();
        Path absolute = root.resolve(relative).normalize();
        if (!absolute.startsWith(root)) {
            throw new SecurityException("Path escapes storage root");
        }
        return absolute;
    }
}
