package com.mirceone.inventoryapp.api.inventory;

import java.util.UUID;

public record BuyListItemResponse(
        UUID id,
        String name,
        String sku,
        int currentQuantity,
        int effectiveMinThreshold,
        /** Units needed to reach the minimum (min minus current). */
        int shortfall
) {
}
