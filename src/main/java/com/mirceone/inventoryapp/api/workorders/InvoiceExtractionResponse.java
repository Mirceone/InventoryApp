package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceExtractionResponse(
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
