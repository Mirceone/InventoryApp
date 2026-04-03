package com.mirceone.inventoryapp.api.inventory;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        int currentQuantity
) {
}
