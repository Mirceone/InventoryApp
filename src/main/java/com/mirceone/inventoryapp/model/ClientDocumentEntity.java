package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "client_documents",
        indexes = @Index(name = "idx_client_documents_firm_client", columnList = "firm_id, client_id")
)
public class ClientDocumentEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "stored_path", nullable = false, length = 1024)
    private String storedPath;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClientDocumentEntity() {
    }

    public ClientDocumentEntity(
            UUID firmId,
            UUID clientId,
            String storedPath,
            String originalFilename,
            String contentType,
            long sizeBytes,
            UUID uploadedByUserId
    ) {
        this.firmId = firmId;
        this.clientId = clientId;
        this.storedPath = storedPath;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedByUserId = uploadedByUserId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
