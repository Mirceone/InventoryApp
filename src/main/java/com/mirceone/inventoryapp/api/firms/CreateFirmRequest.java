package com.mirceone.inventoryapp.api.firms;

import jakarta.validation.constraints.NotBlank;

public record CreateFirmRequest(
        @NotBlank String name
) {
}
