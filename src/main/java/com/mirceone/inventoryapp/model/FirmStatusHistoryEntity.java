package com.mirceone.inventoryapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "firm_status_history")
public class FirmStatusHistoryEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private FirmStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 32)
    private FirmStatus newStatus;

    @Column(name = "message", length = 512)
    private String message;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private FirmStatusChangeSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmStatusHistoryEntity() {
    }

    public FirmStatusHistoryEntity(
            UUID firmId,
            FirmStatus previousStatus,
            FirmStatus newStatus,
            String message,
            UUID actorUserId,
            FirmStatusChangeSource source
    ) {
        this.firmId = firmId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.message = message;
        this.actorUserId = actorUserId;
        this.source = source;
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

    public FirmStatus getPreviousStatus() {
        return previousStatus;
    }

    public FirmStatus getNewStatus() {
        return newStatus;
    }

    public String getMessage() {
        return message;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public FirmStatusChangeSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
