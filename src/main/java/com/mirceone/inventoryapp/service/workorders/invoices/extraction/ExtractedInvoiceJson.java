package com.mirceone.inventoryapp.service.workorders.invoices.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Product-shaped JSON the VLM is asked to return from an invoice image: only the fields needed to
 * create products. Quantity is kept as String so a locale-formatted value does not fail the parse
 * ({@link InvoiceValueParser} converts it leniently).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedInvoiceJson(List<Product> products) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
            String name,
            String sku,
            String quantity
    ) {
    }
}
