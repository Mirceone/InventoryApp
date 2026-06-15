package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID workOrderId,
        String displayName,
        String extension,
        String mimeType,
        long sizeBytes,
        InvoiceProcessingStatus processingStatus,
        String markdownText,
        String processingError,
        Instant createdAt,
        Instant processedAt,
        UUID uploadedByUserId,
        String uploadedByEmail
) {
}
