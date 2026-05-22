package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.OrganizationSource;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal read model for firm-level electronic folder listing (includes uploader email).
 */
public record DocumentSummary(
        UUID id,
        UUID firmId,
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
