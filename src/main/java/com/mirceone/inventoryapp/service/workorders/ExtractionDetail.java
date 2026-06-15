package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Product candidates extracted from an invoice, as exposed to the API. The raw model JSON is
 * included for traceability.
 */
public record ExtractionDetail(
        UUID id,
        UUID invoiceId,
        InvoiceExtractionStatus status,
        String error,
        Instant extractedAt,
        String rawJson,
        List<Product> products
) {

    public record Product(
            UUID id,
            int lineNo,
            String name,
            String sku,
            BigDecimal quantity
    ) {
    }
}
