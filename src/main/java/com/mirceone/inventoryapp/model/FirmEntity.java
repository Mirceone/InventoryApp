package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * {@link #ownerUserId} mirrors the firm's OWNER at creation; keep in sync on ownership transfer.
 * Permission checks use {@code firm_members.role}, not this column alone.
 */
@Entity
@Table(name = "firms")
public class FirmEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FirmStatus status;

    @Column(name = "status_message", length = 512)
    private String statusMessage;

    @Column(name = "status_updated_at", nullable = false)
    private Instant statusUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmEntity() {
    }

    public FirmEntity(UUID ownerUserId, String name) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.status = FirmStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.statusUpdatedAt == null) {
            this.statusUpdatedAt = now;
        }
        if (this.status == null) {
            this.status = FirmStatus.ACTIVE;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FirmStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Instant getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatus(FirmStatus status) {
        this.status = status;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setStatusUpdatedAt(Instant statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }
}
