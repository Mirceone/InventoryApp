package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;

import java.util.List;
import java.util.UUID;

public record BatchInvoiceUploadResult(
        List<BatchInvoiceUploadItem> accepted,
        List<BatchInvoiceUploadError> errors
) {
    public record BatchInvoiceUploadItem(
            UUID id,
            String displayName,
            InvoiceProcessingStatus processingStatus
    ) {
    }

    public record BatchInvoiceUploadError(
            String originalFilename,
            String message
    ) {
    }
}
