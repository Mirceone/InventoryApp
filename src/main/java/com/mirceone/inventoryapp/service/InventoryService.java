package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.inventory.*;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final FirmService firmService;

    public InventoryService(ProductRepository productRepository, FirmService firmService) {
        this.productRepository = productRepository;
        this.firmService = firmService;
    }

    public ProductResponse createProduct(UUID userId, UUID firmId, CreateProductRequest request) {
        firmService.assertUserIsMember(firmId, userId);

        ProductEntity product = new ProductEntity(
                firmId,
                request.name(),
                request.sku(),
                request.initialQuantity()
        );
        product = productRepository.save(product);

        return toResponse(product);
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

        product.setCurrentQuantity(request.quantity());
        product = productRepository.save(product);

        return toResponse(product);
    }

    public ProductResponse adjustStock(UUID userId, UUID firmId, UUID productId, AdjustStockRequest request) {
        firmService.assertUserIsMember(firmId, userId);

        ProductEntity product = productRepository.findByIdAndFirmId(productId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int newQuantity = product.getCurrentQuantity() + request.delta();
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        product.setCurrentQuantity(newQuantity);
        product = productRepository.save(product);

        return toResponse(product);
    }

    private ProductResponse toResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity()
        );
    }
}
