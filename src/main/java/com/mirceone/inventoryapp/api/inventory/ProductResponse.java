package com.mirceone.inventoryapp.api.inventory;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        int currentQuantity,
        boolean reorderEnabled,
        /** Explicit threshold, or null to use application default. */
        Integer reorderThreshold,
        UUID categoryId,
        String categoryName,
        String imgUrl
) {
}
