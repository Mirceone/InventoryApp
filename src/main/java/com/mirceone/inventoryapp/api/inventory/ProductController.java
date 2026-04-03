package com.mirceone.inventoryapp.api.inventory;

import com.mirceone.inventoryapp.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/products")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ProductResponse createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.createProduct(userId, firmId, request);
    }

    @GetMapping
    public List<ProductResponse> listProducts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.listProducts(userId, firmId);
    }

    @PutMapping("/{productId}/stock")
    public ProductResponse setStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID productId,
            @Valid @RequestBody SetStockRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.setStock(userId, firmId, productId, request);
    }

    @PostMapping("/{productId}/stock/adjust")
    public ProductResponse adjustStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.adjustStock(userId, firmId, productId, request);
    }
}
