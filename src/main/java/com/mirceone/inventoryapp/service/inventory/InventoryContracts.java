package com.mirceone.inventoryapp.service.inventory;

import java.util.UUID;

/**
 * Service-layer shapes for catalog & stock; independent of HTTP DTOs.
 */
public final class InventoryContracts {

    private InventoryContracts() {
    }

    public record ProductSummary(
            UUID id,
            String name,
            String sku,
            int currentQuantity,
            boolean reorderEnabled,
            Integer reorderThreshold,
            UUID categoryId,
            String categoryName,
            String imgUrl,
            UUID preferredRouteStopId
    ) {
    }

    public record BuyListLine(
            UUID id,
            String name,
            String sku,
            int currentQuantity,
            int effectiveMinThreshold,
            int shortfall,
            UUID categoryId,
            String categoryName,
            UUID preferredRouteStopId
    ) {
    }

    public record CreateProductSpec(
            String name,
            String sku,
            int initialQuantity,
            Boolean reorderEnabled,
            Integer reorderThreshold,
            UUID categoryId,
            String imgUrl,
            UUID preferredRouteStopId
    ) {
    }

    public record UpdateProductSpec(
            String name,
            String sku,
            Boolean reorderEnabled,
            Integer reorderThreshold,
            UUID categoryId,
            String imgUrl,
            UUID preferredRouteStopId,
            Boolean clearPreferredRouteStop
    ) {
    }

    public record CategorySummary(UUID id, String name) {
    }
}
