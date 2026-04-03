package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.inventory.*;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.model.StockChangeEventEntity;
import com.mirceone.inventoryapp.model.StockChangeType;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.repository.StockChangeEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockChangeEventRepository stockChangeEventRepository;
    private final FirmService firmService;
    private final int defaultReorderThreshold;

    public InventoryService(
            ProductRepository productRepository,
            StockChangeEventRepository stockChangeEventRepository,
            FirmService firmService,
            @Value("${app.inventory.default-reorder-threshold:4}") int defaultReorderThreshold
    ) {
        this.productRepository = productRepository;
        this.stockChangeEventRepository = stockChangeEventRepository;
        this.firmService = firmService;
        this.defaultReorderThreshold = defaultReorderThreshold;
    }

    public ProductResponse createProduct(UUID userId, UUID firmId, CreateProductRequest request) {
        firmService.assertUserIsMember(firmId, userId);

        boolean reorderEnabled = request.reorderEnabled() != null ? request.reorderEnabled() : true;
        ProductEntity product = new ProductEntity(
                firmId,
                request.name(),
                request.sku(),
                request.initialQuantity(),
                reorderEnabled,
                request.reorderThreshold()
        );
        product = productRepository.save(product);

        return toResponse(product);
    }

    public ProductResponse updateProduct(UUID userId, UUID firmId, UUID productId, UpdateProductRequest request) {
        firmService.assertUserIsMember(firmId, userId);
        if (request.name() == null && request.sku() == null && request.reorderEnabled() == null && request.reorderThreshold() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        ProductEntity product = productRepository.findByIdAndFirmId(productId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.sku() != null) {
            product.setSku(request.sku());
        }
        if (request.reorderEnabled() != null) {
            product.setReorderEnabled(request.reorderEnabled());
        }
        if (request.reorderThreshold() != null) {
            product.setReorderThreshold(request.reorderThreshold());
        }

        product = productRepository.save(product);
        return toResponse(product);
    }

    public List<BuyListItemResponse> listBuyList(UUID userId, UUID firmId) {
        firmService.assertUserIsMember(firmId, userId);

        return productRepository.findNeedingRestock(firmId, defaultReorderThreshold).stream()
                .map(this::toBuyListItem)
                .toList();
    }

    public List<ProductResponse> listProducts(UUID userId, UUID firmId) {
        firmService.assertUserIsMember(firmId, userId);

        return productRepository.findAllByFirmId(firmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse setStock(UUID userId, UUID firmId, UUID productId, SetStockRequest request) {
        firmService.assertUserIsMember(firmId, userId);

        ProductEntity product = productRepository.findByIdAndFirmId(productId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int previousQuantity = product.getCurrentQuantity();
        int newQuantity = request.quantity();
        product.setCurrentQuantity(request.quantity());
        product = productRepository.save(product);
        saveStockEvent(userId, firmId, product.getId(), StockChangeType.SET, previousQuantity, newQuantity);

        return toResponse(product);
    }

    public ProductResponse adjustStock(UUID userId, UUID firmId, UUID productId, AdjustStockRequest request) {
        firmService.assertUserIsMember(firmId, userId);

        ProductEntity product = productRepository.findByIdAndFirmId(productId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int previousQuantity = product.getCurrentQuantity();
        int newQuantity = previousQuantity + request.delta();
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        product.setCurrentQuantity(newQuantity);
        product = productRepository.save(product);
        saveStockEvent(userId, firmId, product.getId(), StockChangeType.ADJUST, previousQuantity, newQuantity);

        return toResponse(product);
    }

    private void saveStockEvent(
            UUID actorUserId,
            UUID firmId,
            UUID productId,
            StockChangeType changeType,
            int previousQuantity,
            int newQuantity
    ) {
        StockChangeEventEntity event = new StockChangeEventEntity(
                firmId,
                productId,
                actorUserId,
                changeType,
                previousQuantity,
                newQuantity,
                newQuantity - previousQuantity
        );
        stockChangeEventRepository.save(event);
    }

    private ProductResponse toResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity(),
                product.isReorderEnabled(),
                product.getReorderThreshold()
        );
    }

    private BuyListItemResponse toBuyListItem(ProductEntity product) {
        int effective = effectiveMinThreshold(product);
        int shortfall = effective - product.getCurrentQuantity();
        return new BuyListItemResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity(),
                effective,
                shortfall
        );
    }

    int effectiveMinThreshold(ProductEntity product) {
        return product.getReorderThreshold() != null ? product.getReorderThreshold() : defaultReorderThreshold;
    }
}
