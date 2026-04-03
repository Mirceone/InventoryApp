package com.mirceone.inventoryapp.api.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank String name,
        @Size(max = 128) String sku,
        @Min(0) int initialQuantity,
        /** When null, defaults to true. */
        Boolean reorderEnabled,
        /** When null, application default threshold is used at runtime. */
        @Min(0) Integer reorderThreshold
) {
}
