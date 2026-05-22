package com.mirceone.inventoryapp.api.firms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFirmRequest(
        @NotBlank @Size(max = 255) String name
) {
}
