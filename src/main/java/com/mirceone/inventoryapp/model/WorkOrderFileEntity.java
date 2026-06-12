package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * File metadata. Folder membership is the {@code folderId} FK; the blob lives under an
 * opaque {@code storageKey} that never changes after upload.
 */
@Entity
@Table(name = "work_order_files")
public class WorkOrderFileEntity {

    @Id
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkOrderFileEntity() {
    }

    public WorkOrderFileEntity(
            UUID id,
            UUID firmId,
            UUID workOrderId,
            UUID folderId,
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
        this.folderId = folderId;
        this.uploadedByUserId = uploadedByUserId;
        this.displayName = displayName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.storageKey = storageKey;
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

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public void setFolderId(UUID folderId) {
        this.folderId = folderId;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
