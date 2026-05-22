package com.mirceone.inventoryapp.api.inventory;

import com.mirceone.inventoryapp.service.inventory.InventoryContracts;

import java.util.List;

public final class InventoryWebMapper {

    private InventoryWebMapper() {
    }

    public static InventoryContracts.CreateProductSpec toCreateProductSpec(CreateProductRequest request) {
        return new InventoryContracts.CreateProductSpec(
                request.name(),
                request.sku(),
                request.initialQuantity(),
                request.reorderEnabled(),
                request.reorderThreshold(),
                request.categoryId(),
                request.imgUrl(),
                request.preferredRouteStopId()
        );
    }

    public static InventoryContracts.UpdateProductSpec toUpdateProductSpec(UpdateProductRequest request) {
        return new InventoryContracts.UpdateProductSpec(
                request.name(),
                request.sku(),
                request.reorderEnabled(),
                request.reorderThreshold(),
                request.categoryId(),
                request.imgUrl(),
                request.preferredRouteStopId(),
                request.clearPreferredRouteStop()
        );
    }

    public static ProductResponse toProductResponse(InventoryContracts.ProductSummary p) {
        return new ProductResponse(
                p.id(),
                p.name(),
                p.sku(),
                p.currentQuantity(),
                p.reorderEnabled(),
                p.reorderThreshold(),
                p.categoryId(),
                p.categoryName(),
                p.imgUrl(),
                p.preferredRouteStopId()
        );
    }

    public static BuyListItemResponse toBuyListResponse(InventoryContracts.BuyListLine line) {
        return new BuyListItemResponse(
                line.id(),
                line.name(),
                line.sku(),
                line.currentQuantity(),
                line.effectiveMinThreshold(),
                line.shortfall(),
                line.categoryId(),
                line.categoryName(),
                line.preferredRouteStopId()
        );
    }

    public static CategoryResponse toCategoryResponse(InventoryContracts.CategorySummary c) {
        return new CategoryResponse(c.id(), c.name());
    }

    public static List<ProductResponse> toProductResponseList(List<InventoryContracts.ProductSummary> items) {
        return items.stream().map(InventoryWebMapper::toProductResponse).toList();
    }

    public static List<BuyListItemResponse> toBuyListResponseList(List<InventoryContracts.BuyListLine> items) {
        return items.stream().map(InventoryWebMapper::toBuyListResponse).toList();
    }

    public static List<CategoryResponse> toCategoryResponseList(List<InventoryContracts.CategorySummary> items) {
        return items.stream().map(InventoryWebMapper::toCategoryResponse).toList();
    }
}
