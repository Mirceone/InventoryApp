package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.inventory.AdjustStockRequest;
import com.mirceone.inventoryapp.api.inventory.BuyListItemResponse;
import com.mirceone.inventoryapp.api.inventory.CreateProductRequest;
import com.mirceone.inventoryapp.api.inventory.ProductResponse;
import com.mirceone.inventoryapp.api.inventory.SetStockRequest;
import com.mirceone.inventoryapp.api.inventory.UpdateProductRequest;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.repository.StockChangeEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockChangeEventRepository stockChangeEventRepository;
    @Mock
    private FirmService firmService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(productRepository, stockChangeEventRepository, firmService, 4);
    }

    @Test
    void createProductReturnsCreatedProduct() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Laptop", "SKU-1", 10, null, null);
        ProductEntity saved = new ProductEntity(firmId, "Laptop", "SKU-1", 10);

        when(productRepository.save(any(ProductEntity.class))).thenReturn(saved);

        ProductResponse response = inventoryService.createProduct(userId, firmId, request);

        assertEquals("Laptop", response.name());
        assertEquals("SKU-1", response.sku());
        assertEquals(10, response.currentQuantity());
    }

    @Test
    void setStockWhenProductMissingThrowsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndFirmId(productId, firmId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.setStock(userId, firmId, productId, new SetStockRequest(5))
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void adjustStockBelowZeroThrowsBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(firmId, "Mouse", "SKU-2", 1);

        when(productRepository.findByIdAndFirmId(productId, firmId)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.adjustStock(userId, firmId, productId, new AdjustStockRequest(-2))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void setStockCreatesAuditEvent() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(firmId, "Keyboard", "KB-1", 10);

        when(productRepository.findByIdAndFirmId(productId, firmId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);

        inventoryService.setStock(userId, firmId, productId, new SetStockRequest(20));

        verify(stockChangeEventRepository).save(any());
    }

    @Test
    void adjustStockCreatesAuditEvent() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(firmId, "Mouse", "MS-1", 10);

        when(productRepository.findByIdAndFirmId(productId, firmId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);

        inventoryService.adjustStock(userId, firmId, productId, new AdjustStockRequest(-3));

        verify(stockChangeEventRepository).save(any());
    }

    @Test
    void listBuyListReturnsProductsBelowThreshold() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID lowId = UUID.randomUUID();
        ProductEntity low = new ProductEntity(firmId, "Low", "L-1", 2, true, null);
        when(productRepository.findNeedingRestock(firmId, 4)).thenReturn(List.of(low));

        List<BuyListItemResponse> list = inventoryService.listBuyList(userId, firmId);

        assertEquals(1, list.size());
        assertEquals(4, list.getFirst().effectiveMinThreshold());
        assertEquals(2, list.getFirst().shortfall());
        assertEquals(2, list.getFirst().currentQuantity());
    }

    @Test
    void effectiveMinThresholdUsesProductOverride() {
        ProductEntity p = new ProductEntity(UUID.randomUUID(), "P", "S", 0, true, 10);
        assertEquals(10, inventoryService.effectiveMinThreshold(p));
    }

    @Test
    void effectiveMinThresholdUsesDefaultWhenNull() {
        ProductEntity p = new ProductEntity(UUID.randomUUID(), "P", "S", 0, true, null);
        assertEquals(4, inventoryService.effectiveMinThreshold(p));
    }

    @Test
    void updateProductWithEmptyBodyThrowsBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.updateProduct(userId, firmId, productId,
                        new UpdateProductRequest(null, null, null, null))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
