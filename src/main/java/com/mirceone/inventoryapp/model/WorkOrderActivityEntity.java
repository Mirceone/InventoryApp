package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "work_order_activity",
        indexes = @Index(
                name = "idx_work_order_activity_work_order_created",
                columnList = "work_order_id, created_at")
)
public class WorkOrderActivityEntity {

    @Id
    private UUID id;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkOrderActivityEntity() {
    }

    public WorkOrderActivityEntity(
            UUID workOrderId,
            UUID firmId,
            String title,
            String description,
            UUID createdByUserId
    ) {
        this.id = UUID.randomUUID();
        this.workOrderId = workOrderId;
        this.firmId = firmId;
        this.title = title;
        this.description = description;
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

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
