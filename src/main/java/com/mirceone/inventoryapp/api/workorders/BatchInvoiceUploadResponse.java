package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;

import java.util.List;
import java.util.UUID;

public record BatchInvoiceUploadResponse(
        List<BatchInvoiceUploadItemResponse> accepted,
        List<BatchInvoiceUploadErrorResponse> errors
) {
    public record BatchInvoiceUploadItemResponse(
            UUID id,
            String displayName,
            InvoiceProcessingStatus processingStatus
    ) {
    }

    public record BatchInvoiceUploadErrorResponse(String originalFilename, String message) {
    }
}
