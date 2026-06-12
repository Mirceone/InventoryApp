package com.mirceone.inventoryapp.service.workorders.invoices.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Raw shape of the JSON the LLM is asked to return. Numeric fields are kept as String so a
 * locale-formatted or currency-decorated value from the model does not fail the whole parse;
 * {@link InvoiceValueParser} converts them leniently.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedInvoiceJson(
        String supplier,
        String invoiceNumber,
        String invoiceDate,
        String currency,
        String total,
        List<Line> lineItems
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(
            String description,
            String sku,
            String quantity,
            String unit,
            String unitPrice,
            String lineTotal
    ) {
    }
}
