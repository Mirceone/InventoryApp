package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;

import java.util.List;
import java.util.UUID;

public record BatchUploadResponse(
        List<BatchUploadItemResponse> accepted,
        List<BatchUploadErrorResponse> errors
) {
    public record BatchUploadItemResponse(
            UUID id,
            String originalFilename,
            DocumentProcessingStatus processingStatus
    ) {
    }

    public record BatchUploadErrorResponse(String originalFilename, String message) {
    }
}
