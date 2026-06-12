package com.mirceone.inventoryapp.service.workorders;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal read model for file listings (includes uploader email and the materialized folder path).
 */
public record FileSummary(
        UUID id,
        UUID firmId,
        UUID workOrderId,
        UUID folderId,
        String folderPath,
        String displayName,
        String extension,
        String mimeType,
        long sizeBytes,
        Instant createdAt,
        UUID uploadedByUserId,
        String uploadedByEmail
) {
}
