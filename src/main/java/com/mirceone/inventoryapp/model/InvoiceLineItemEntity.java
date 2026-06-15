package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A product candidate extracted from an invoice: just the fields needed to create/update a
 * {@link ProductEntity} (name, optional SKU, quantity). Matching and applying to inventory are
 * handled by later phases.
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

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "sku", length = 128)
    private String sku;

    @Column(name = "quantity", precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvoiceLineItemEntity() {
    }

    public InvoiceLineItemEntity(
            UUID id,
            UUID extractionId,
            UUID firmId,
            int lineNo,
            String name,
            String sku,
            BigDecimal quantity
    ) {
        this.id = id;
        this.extractionId = extractionId;
        this.firmId = firmId;
        this.lineNo = lineNo;
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
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

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
