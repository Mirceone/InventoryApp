package com.mirceone.inventoryapp.service.storage;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Component
public class LocalBlobStorage implements BlobStorage {

    private final AppIntegrationProperties props;

    public LocalBlobStorage(AppIntegrationProperties props) {
        this.props = props;
    }

    @Override
    public long store(String key, InputStream content, long expectedSize) throws IOException {
        Path target = resolveUnderRoot(key);
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
    public Resource open(String key) throws IOException {
        Path target = resolveUnderRoot(key);
        if (!Files.isRegularFile(target)) {
            throw new FileNotFoundException("No blob for key: " + key);
        }
        return new FileSystemResource(target.toFile());
    }

    @Override
    public void delete(String key) throws IOException {
        Path target = resolveUnderRoot(key);
        if (Files.deleteIfExists(target)) {
            cleanupParent(target);
        }
    }

    @Override
    public void deleteByPrefix(String prefix) throws IOException {
        Path dir = resolveUnderRoot(prefix);
        if (!Files.isDirectory(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(d);
                return FileVisitResult.CONTINUE;
            }
        });
        cleanupParent(dir);
    }

    private void cleanupParent(Path file) {
        Path root = storageRoot();
        Path parent = file.getParent();
        if (parent != null && !parent.equals(root)) {
            try {
                Files.deleteIfExists(parent);
            } catch (IOException ignored) {
                // directory not empty or still in use — ignore
            }
        }
    }

    private Path storageRoot() {
        return Path.of(props.getStorage().getRoot()).toAbsolutePath().normalize();
    }

    private Path resolveUnderRoot(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("storage key required");
        }
        if (key.contains("..") || key.startsWith("/") || key.codePointAt(0) == 92) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        Path root = storageRoot();
        Path relative = Path.of(key.replace('/', java.io.File.separatorChar)).normalize();
        Path absolute = root.resolve(relative).normalize();
        if (!absolute.startsWith(root)) {
            throw new SecurityException("Path escapes storage root");
        }
        return absolute;
    }
}
