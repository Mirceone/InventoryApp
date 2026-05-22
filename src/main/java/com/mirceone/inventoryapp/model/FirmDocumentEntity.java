package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "firm_documents",
        indexes = {
                @Index(name = "idx_firm_documents_firm_created", columnList = "firm_id, created_at"),
                @Index(name = "idx_firm_documents_firm_folder_created", columnList = "firm_id, folder_path, created_at"),
                @Index(name = "idx_firm_documents_dossier_folder_created", columnList = "dossier_id, folder_path, created_at")
        }
)
public class FirmDocumentEntity {

    @Id
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 1024, unique = true)
    private String storageKey;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "folder_path", length = 512)
    private String folderPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private DocumentProcessingStatus processingStatus = DocumentProcessingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_source", length = 16)
    private OrganizationSource organizationSource;

    @Column(name = "organization_error", columnDefinition = "text")
    private String organizationError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmDocumentEntity() {
    }

    /** New upload awaiting background organization. */
    public FirmDocumentEntity(
            UUID id,
            UUID firmId,
            UUID dossierId,
            UUID uploadedByUserId,
            String originalFilename,
            String mimeType,
            long sizeBytes,
            String storageKey,
            String checksumSha256
    ) {
        this.id = id;
        this.firmId = firmId;
        this.dossierId = dossierId;
        this.uploadedByUserId = uploadedByUserId;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.checksumSha256 = checksumSha256;
        this.processingStatus = DocumentProcessingStatus.PENDING;
        this.folderPath = null;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (processingStatus == null) {
            processingStatus = DocumentProcessingStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getDossierId() {
        return dossierId;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public DocumentProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(DocumentProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public OrganizationSource getOrganizationSource() {
        return organizationSource;
    }

    public void setOrganizationSource(OrganizationSource organizationSource) {
        this.organizationSource = organizationSource;
    }

    public String getOrganizationError() {
        return organizationError;
    }

    public void setOrganizationError(String organizationError) {
        this.organizationError = organizationError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
