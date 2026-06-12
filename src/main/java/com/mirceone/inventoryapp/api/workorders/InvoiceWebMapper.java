package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.service.workorders.BatchInvoiceUploadResult;
import com.mirceone.inventoryapp.service.workorders.ExtractionDetail;
import com.mirceone.inventoryapp.service.workorders.InvoiceSummary;
import org.springframework.data.domain.Page;

import java.util.List;

public final class InvoiceWebMapper {

    private InvoiceWebMapper() {
    }

    public static InvoiceExtractionResponse toExtractionResponse(ExtractionDetail detail) {
        List<InvoiceExtractionResponse.LineItem> lines = detail.lineItems().stream()
                .map(l -> new InvoiceExtractionResponse.LineItem(
                        l.id(), l.lineNo(), l.rawDescription(), l.sku(),
                        l.quantity(), l.unit(), l.unitPrice(), l.lineTotal()))
                .toList();
        return new InvoiceExtractionResponse(
                detail.id(),
                detail.invoiceId(),
                detail.status(),
                detail.supplierName(),
                detail.invoiceNumber(),
                detail.invoiceDate(),
                detail.currency(),
                detail.totalAmount(),
                detail.error(),
                detail.extractedAt(),
                lines
        );
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
