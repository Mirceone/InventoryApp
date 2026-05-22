package com.mirceone.inventoryapp.service.documents.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Persists binary document content outside the database (filesystem now; S3-compatible later).
 */
public interface DocumentStorage {

    /**
     * @return number of bytes written
     */
    long store(String storageKey, InputStream content, long expectedSize) throws IOException;

    void delete(String storageKey) throws IOException;

    void move(String fromKey, String toKey) throws IOException;

    Resource asResource(String storageKey) throws IOException;
}
