package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceExtractionResponse(
        UUID id,
        UUID invoiceId,
        InvoiceExtractionStatus status,
        String supplierName,
        String invoiceNumber,
        LocalDate invoiceDate,
        String currency,
        BigDecimal totalAmount,
        String error,
        Instant extractedAt,
        List<LineItem> lineItems
) {

    public record LineItem(
            UUID id,
            int lineNo,
            String rawDescription,
            String sku,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
