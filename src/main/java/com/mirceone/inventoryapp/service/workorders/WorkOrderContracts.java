package com.mirceone.inventoryapp.service.workorders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class WorkOrderContracts {

    private WorkOrderContracts() {
    }

    public record CreateWorkOrderSpec(
            String name,
            String clientName,
            String location,
            String description,
            LocalDate estimatedEndDate
    ) {
    }

    public record UpdateWorkOrderSpec(
            String name,
            String clientName,
            String location,
            String description,
            LocalDate estimatedEndDate,
            boolean clearDescription
    ) {
    }

    public record CreateFolderSpec(
            UUID parentId,
            String name,
            List<String> extensions
    ) {
    }

    /**
     * Partial folder update; {@code parentPresent} distinguishes "move to root" (present, null)
     * from "leave parent unchanged" (absent).
     */
    public record UpdateFolderSpec(
            String name,
            UUID parentId,
            boolean parentPresent
    ) {
    }

    /**
     * Partial file update: rename and/or manual move to another folder.
     */
    public record UpdateFileSpec(
            String displayName,
            UUID folderId
    ) {
    }

    public record CreateActivitySpec(
            String title,
            String description
    ) {
    }
}
