package com.mirceone.inventoryapp.api.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Partial update: only non-null fields are applied.
 */
public record UpdateProductRequest(
        @Size(max = 255) String name,
        @Size(max = 128) String sku,
        Boolean reorderEnabled,
        @Min(0) Integer reorderThreshold
) {
}
