package com.mirceone.inventoryapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "firm_ownership_transfer_confirmations",
        indexes = {
                @Index(
                        name = "idx_firm_ownership_transfer_confirmations_lookup",
                        columnList = "firm_id, requester_user_id, new_owner_user_id, created_at DESC"
                ),
                @Index(name = "idx_firm_ownership_transfer_confirmations_expires_at", columnList = "expires_at")
        }
)
public class FirmOwnershipTransferConfirmationEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "new_owner_user_id", nullable = false)
    private UUID newOwnerUserId;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmOwnershipTransferConfirmationEntity() {
    }

    public FirmOwnershipTransferConfirmationEntity(
            UUID firmId,
            UUID requesterUserId,
            UUID newOwnerUserId,
            String codeHash,
            Instant expiresAt
    ) {
        this.firmId = firmId;
        this.requesterUserId = requesterUserId;
        this.newOwnerUserId = newOwnerUserId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
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

    public UUID getRequesterUserId() {
        return requesterUserId;
    }

    public UUID getNewOwnerUserId() {
        return newOwnerUserId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
