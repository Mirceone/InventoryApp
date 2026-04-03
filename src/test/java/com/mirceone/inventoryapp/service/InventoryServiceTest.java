package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.inventory.AdjustStockRequest;
import com.mirceone.inventoryapp.api.inventory.CreateProductRequest;
import com.mirceone.inventoryapp.api.inventory.ProductResponse;
import com.mirceone.inventoryapp.api.inventory.SetStockRequest;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private FirmService firmService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(productRepository, firmService);
    }

    @Test
    void createProductReturnsCreatedProduct() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Laptop", "SKU-1", 10);
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
}
