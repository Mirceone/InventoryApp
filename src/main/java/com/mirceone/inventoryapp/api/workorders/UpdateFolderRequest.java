package com.mirceone.inventoryapp.api.workorders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Partial update: {@code parentId} explicitly set to null moves the folder to the root;
 * omitting it leaves the parent unchanged.
 */
public class UpdateFolderRequest {

    @Size(max = 64)
    private String name;

    private UUID parentId;

    private boolean parentPresent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getParentId() {
        return parentId;
    }

    @JsonProperty("parentId")
    public void setParentId(UUID parentId) {
        this.parentId = parentId;
        this.parentPresent = true;
    }

    @JsonIgnore
    public boolean isParentPresent() {
        return parentPresent;
    }
}
