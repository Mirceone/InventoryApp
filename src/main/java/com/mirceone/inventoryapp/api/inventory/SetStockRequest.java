package com.mirceone.inventoryapp.api.inventory;

import jakarta.validation.constraints.Min;

public record SetStockRequest(
        @Min(0) int quantity
) {
}
