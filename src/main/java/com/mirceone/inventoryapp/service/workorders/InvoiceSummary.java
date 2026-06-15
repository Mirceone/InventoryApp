package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record InvoiceSummary(
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
    public static InvoiceSummary listItem(
            UUID id,
            UUID workOrderId,
            String displayName,
            String extension,
            String mimeType,
            long sizeBytes,
            InvoiceProcessingStatus processingStatus,
            Instant createdAt,
            Instant processedAt,
            UUID uploadedByUserId,
            String uploadedByEmail
    ) {
        return new InvoiceSummary(
                id,
                workOrderId,
                displayName,
                extension,
                mimeType,
                sizeBytes,
                processingStatus,
                null,
                null,
                createdAt,
                processedAt,
                uploadedByUserId,
                uploadedByEmail
        );
    }
}
