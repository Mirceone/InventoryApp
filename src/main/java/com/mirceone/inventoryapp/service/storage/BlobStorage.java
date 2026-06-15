package com.mirceone.inventoryapp.service.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Opaque binary blob store. Keys are immutable once written (no rename/move), which keeps
 * implementations trivial on object storage (S3/MinIO) where rename does not exist.
 */
public interface BlobStorage {

    /**
     * @return number of bytes written
     */
    long store(String key, InputStream content, long expectedSize) throws IOException;

    Resource open(String key) throws IOException;

    void delete(String key) throws IOException;

    /**
     * Deletes every blob whose key starts with the given prefix (e.g. a whole work order or firm).
     */
    void deleteByPrefix(String prefix) throws IOException;
}
