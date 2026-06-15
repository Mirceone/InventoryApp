package com.mirceone.inventoryapp.service.workorders;

import java.util.List;
import java.util.UUID;

public record BatchUploadResult(
        List<BatchUploadItem> accepted,
        List<BatchUploadError> errors
) {
    public record BatchUploadItem(
            UUID id,
            String displayName,
            UUID folderId,
            String folderPath
    ) {
    }

    public record BatchUploadError(
            String originalFilename,
            String message
    ) {
    }
}
