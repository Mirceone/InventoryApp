package com.mirceone.inventoryapp.api.inventory;

import com.mirceone.inventoryapp.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/products")
@Tag(name = "Inventory", description = "Product and stock endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @Operation(summary = "Create product in firm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member")
    })
    public ProductResponse createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.createProduct(userId, firmId, request);
    }

    @GetMapping
    @Operation(summary = "List products for firm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member")
    })
    public List<ProductResponse> listProducts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.listProducts(userId, firmId);
    }

    @GetMapping("/buy-list")
    @Operation(summary = "List products below reorder threshold (restock / buy list)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Buy list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member")
    })
    public List<BuyListItemResponse> listBuyList(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.listBuyList(userId, firmId);
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Update product fields (partial)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation or empty body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ProductResponse updateProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.updateProduct(userId, firmId, productId, request);
    }

    @PutMapping("/{productId}/stock")
    @Operation(summary = "Set absolute stock value for product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
    @Operation(summary = "Adjust stock by delta value")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock adjusted"),
            @ApiResponse(responseCode = "400", description = "Validation or business error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
