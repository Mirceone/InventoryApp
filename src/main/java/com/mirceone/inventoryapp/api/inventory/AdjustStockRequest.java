package com.mirceone.inventoryapp.api.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdjustStockRequest(
        @Min(-1000000) @Max(1000000) int delta
) {
}
