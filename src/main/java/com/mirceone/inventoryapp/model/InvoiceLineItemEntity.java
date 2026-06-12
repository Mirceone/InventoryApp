package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One extracted line item from an invoice. Matching against firm products and applying to
 * inventory are deferred to later phases; this entity is the raw structured extraction.
 */
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItemEntity {

    @Id
    private UUID id;

    @Column(name = "extraction_id", nullable = false)
    private UUID extractionId;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "raw_description", nullable = false, columnDefinition = "text")
    private String rawDescription;

    @Column(name = "sku", length = 128)
    private String sku;

    @Column(name = "quantity", precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 18, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvoiceLineItemEntity() {
    }

    public InvoiceLineItemEntity(
            UUID id,
            UUID extractionId,
            UUID firmId,
            int lineNo,
            String rawDescription,
            String sku,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
        this.id = id;
        this.extractionId = extractionId;
        this.firmId = firmId;
        this.lineNo = lineNo;
        this.rawDescription = rawDescription;
        this.sku = sku;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getExtractionId() {
        return extractionId;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public String getRawDescription() {
        return rawDescription;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
