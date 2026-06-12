package com.mirceone.inventoryapp.api.workorders;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
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
