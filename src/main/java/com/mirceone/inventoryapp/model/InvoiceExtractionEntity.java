package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One structured extraction per {@link WorkOrderInvoiceEntity}: status, the raw JSON the VLM
 * returned, and (in {@link InvoiceLineItemEntity}) the product candidates ready for inventory.
 */
@Entity
@Table(name = "invoice_extractions")
public class InvoiceExtractionEntity {

    @Id
    private UUID id;

    @Column(name = "invoice_id", nullable = false, unique = true)
    private UUID invoiceId;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceExtractionStatus status = InvoiceExtractionStatus.PENDING;

    @Column(name = "raw_json", columnDefinition = "text")
    private String rawJson;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "extracted_at")
    private Instant extractedAt;

    protected InvoiceExtractionEntity() {
    }

    public InvoiceExtractionEntity(UUID id, UUID invoiceId, UUID firmId, UUID workOrderId) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.firmId = firmId;
        this.workOrderId = workOrderId;
        this.status = InvoiceExtractionStatus.PENDING;
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

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public InvoiceExtractionStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceExtractionStatus status) {
        this.status = status;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(Instant extractedAt) {
        this.extractedAt = extractedAt;
    }
}
