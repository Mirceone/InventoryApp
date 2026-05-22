package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "firm_dossiers",
        uniqueConstraints = @UniqueConstraint(name = "uk_firm_dossiers_firm_name", columnNames = {"firm_id", "name"}),
        indexes = @Index(name = "idx_firm_dossiers_firm_created", columnList = "firm_id, created_at")
)
public class FirmDossierEntity {

    @Id
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmDossierEntity() {
    }

    public FirmDossierEntity(UUID firmId, String name, UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.firmId = firmId;
        this.name = name;
        this.createdByUserId = createdByUserId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
