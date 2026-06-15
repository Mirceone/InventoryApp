package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_order_invoices")
public class WorkOrderInvoiceEntity {

    @Id
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 16)
    private String extension;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "storage_key", nullable = false, length = 512, unique = true)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 16)
    private InvoiceProcessingStatus processingStatus;

    @Column(name = "markdown_text", columnDefinition = "text")
    private String markdownText;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected WorkOrderInvoiceEntity() {
    }

    public WorkOrderInvoiceEntity(
            UUID id,
            UUID firmId,
            UUID workOrderId,
            UUID uploadedByUserId,
            String displayName,
            String extension,
            String mimeType,
            long sizeBytes,
            String checksumSha256,
            String storageKey
    ) {
        this.id = id;
        this.firmId = firmId;
        this.workOrderId = workOrderId;
        this.uploadedByUserId = uploadedByUserId;
        this.displayName = displayName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.storageKey = storageKey;
        this.processingStatus = InvoiceProcessingStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (processingStatus == null) {
            processingStatus = InvoiceProcessingStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public InvoiceProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(InvoiceProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getMarkdownText() {
        return markdownText;
    }

    public void setMarkdownText(String markdownText) {
        this.markdownText = markdownText;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
