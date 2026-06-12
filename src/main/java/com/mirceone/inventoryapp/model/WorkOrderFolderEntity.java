package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Node in the virtual folder tree of a work order. The tree exists only in the database;
 * blob storage keys never encode folder structure.
 */
@Entity
@Table(name = "work_order_folders")
public class WorkOrderFolderEntity {

    @Id
    private UUID id;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "catch_all", nullable = false)
    private boolean catchAll;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkOrderFolderEntity() {
    }

    public WorkOrderFolderEntity(UUID workOrderId, UUID parentId, String name, boolean catchAll, int sortOrder) {
        this.id = UUID.randomUUID();
        this.workOrderId = workOrderId;
        this.parentId = parentId;
        this.name = name;
        this.catchAll = catchAll;
        this.sortOrder = sortOrder;
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

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCatchAll() {
        return catchAll;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
