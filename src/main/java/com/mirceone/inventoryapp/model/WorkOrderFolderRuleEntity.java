package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Maps one normalized file extension (lowercase, no leading dot) to one folder.
 * Unique per (workOrderId, extension) so classification is deterministic.
 */
@Entity
@Table(
        name = "work_order_folder_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_order_folder_rules_extension",
                columnNames = {"work_order_id", "extension"}
        )
)
public class WorkOrderFolderRuleEntity {

    @Id
    private UUID id;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

    @Column(nullable = false, length = 16)
    private String extension;

    protected WorkOrderFolderRuleEntity() {
    }

    public WorkOrderFolderRuleEntity(UUID workOrderId, UUID folderId, String extension) {
        this.id = UUID.randomUUID();
        this.workOrderId = workOrderId;
        this.folderId = folderId;
        this.extension = extension;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public String getExtension() {
        return extension;
    }
}
