package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Header of the structured data extracted from one invoice's markdown.
 * One row per {@link WorkOrderInvoiceEntity}; line items live in {@link InvoiceLineItemEntity}.
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

    @Column(name = "supplier_name", length = 512)
    private String supplierName;

    @Column(name = "invoice_number", length = 128)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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
