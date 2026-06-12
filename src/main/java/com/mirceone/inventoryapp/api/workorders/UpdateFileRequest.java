package com.mirceone.inventoryapp.api.workorders;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Partial update: rename ({@code displayName}) and/or manual move to another folder ({@code folderId}).
 */
public record UpdateFileRequest(
        @Size(max = 255) String displayName,
        UUID folderId
) {
}
