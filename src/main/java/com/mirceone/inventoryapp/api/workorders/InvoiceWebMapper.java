package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.service.workorders.BatchInvoiceUploadResult;
import com.mirceone.inventoryapp.service.workorders.InvoiceSummary;
import org.springframework.data.domain.Page;

import java.util.List;

public final class InvoiceWebMapper {

    private InvoiceWebMapper() {
    }

    public static InvoiceResponse toResponse(InvoiceSummary summary) {
        return new InvoiceResponse(
                summary.id(),
                summary.workOrderId(),
                summary.displayName(),
                summary.extension(),
                summary.mimeType(),
                summary.sizeBytes(),
                summary.processingStatus(),
                summary.markdownText(),
                summary.processingError(),
                summary.createdAt(),
                summary.processedAt(),
                summary.uploadedByUserId(),
                summary.uploadedByEmail()
        );
    }

    public static Page<InvoiceResponse> toResponsePage(Page<InvoiceSummary> page) {
        return page.map(InvoiceWebMapper::toResponse);
    }

    public static BatchInvoiceUploadResponse toBatchResponse(BatchInvoiceUploadResult result) {
        List<BatchInvoiceUploadResponse.BatchInvoiceUploadItemResponse> accepted = result.accepted().stream()
                .map(item -> new BatchInvoiceUploadResponse.BatchInvoiceUploadItemResponse(
                        item.id(), item.displayName(), item.processingStatus()))
                .toList();
        List<BatchInvoiceUploadResponse.BatchInvoiceUploadErrorResponse> errors = result.errors().stream()
                .map(error -> new BatchInvoiceUploadResponse.BatchInvoiceUploadErrorResponse(
                        error.originalFilename(), error.message()))
                .toList();
        return new BatchInvoiceUploadResponse(accepted, errors);
    }
}
