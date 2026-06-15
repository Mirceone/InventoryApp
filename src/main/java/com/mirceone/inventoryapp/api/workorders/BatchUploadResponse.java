package com.mirceone.inventoryapp.api.workorders;

import java.util.List;
import java.util.UUID;

public record BatchUploadResponse(
        List<BatchUploadItemResponse> accepted,
        List<BatchUploadErrorResponse> errors
) {
    public record BatchUploadItemResponse(
            UUID id,
            String displayName,
            UUID folderId,
            String folderPath
    ) {
    }

    public record BatchUploadErrorResponse(String originalFilename, String message) {
    }
}
