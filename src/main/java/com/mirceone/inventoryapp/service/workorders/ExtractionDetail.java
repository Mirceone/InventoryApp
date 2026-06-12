package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Structured data extracted from an invoice (header + line items), as exposed to the API.
 */
public record ExtractionDetail(
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
        List<Line> lineItems
) {

    public record Line(
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
