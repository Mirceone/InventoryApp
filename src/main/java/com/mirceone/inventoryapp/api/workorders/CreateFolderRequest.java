package com.mirceone.inventoryapp.api.workorders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateFolderRequest(
        UUID parentId,
        @NotBlank @Size(max = 64) String name,
        List<String> extensions
) {
}
