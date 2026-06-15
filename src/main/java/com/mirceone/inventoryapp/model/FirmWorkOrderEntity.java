package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import com.mirceone.inventoryapp.model.WorkOrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "firm_work_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_firm_work_orders_firm_name", columnNames = {"firm_id", "name"}),
        indexes = @Index(name = "idx_firm_work_orders_firm_created", columnList = "firm_id, created_at")
)
public class FirmWorkOrderEntity {

    @Id
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "estimated_end_date", nullable = false)
    private LocalDate estimatedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkOrderStatus status;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmWorkOrderEntity() {
    }

    public FirmWorkOrderEntity(
            UUID firmId,
            String name,
            String clientName,
            String location,
            String description,
            LocalDate estimatedEndDate,
            UUID createdByUserId
    ) {
        this.id = UUID.randomUUID();
        this.firmId = firmId;
        this.name = name;
        this.clientName = clientName;
        this.location = location;
        this.description = description;
        this.estimatedEndDate = estimatedEndDate;
        this.status = WorkOrderStatus.PLANNED;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getEstimatedEndDate() {
        return estimatedEndDate;
    }

    public void setEstimatedEndDate(LocalDate estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
