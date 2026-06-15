package com.mirceone.inventoryapp.api.workorders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateActivityRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String description
) {
}
