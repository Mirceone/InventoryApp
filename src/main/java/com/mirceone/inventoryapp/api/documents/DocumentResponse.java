package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.OrganizationSource;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID dossierId,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        Instant createdAt,
        UUID uploadedByUserId,
        String uploadedByEmail,
        String folderPath,
        DocumentProcessingStatus processingStatus,
        OrganizationSource organizationSource
) {
}
