package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;

import java.util.UUID;

public record BatchUploadItem(
        UUID id,
        String originalFilename,
        DocumentProcessingStatus processingStatus
) {
}
