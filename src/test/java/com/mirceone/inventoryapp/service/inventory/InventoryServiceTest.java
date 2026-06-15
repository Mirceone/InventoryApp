package com.mirceone.inventoryapp.service.inventory;

import com.mirceone.inventoryapp.model.CategoryConstants;
import com.mirceone.inventoryapp.model.CategoryEntity;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.repository.CategoryRepository;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.repository.RouteStopRepository;
import com.mirceone.inventoryapp.repository.StockChangeEventRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockChangeEventRepository stockChangeEventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FirmAccessService firmAccessService;
    @Mock
    private RouteStopRepository routeStopRepository;
    @Mock
    private NotificationService notificationService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                productRepository,
                stockChangeEventRepository,
                categoryRepository,
                firmAccessService,
                routeStopRepository,
                notificationService,
                4
        );
    }

    @Test
    void createProductReturnsCreatedProduct() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        InventoryContracts.CreateProductSpec spec =
                new InventoryContracts.CreateProductSpec("Laptop", "SKU-1", 10, null, null, null, null, null);
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity saved = new ProductEntity(firmId, "Laptop", "SKU-1", 10, true, null, misc, null, null);

        when(categoryRepository.findByFirmIdAndName(firmId, CategoryConstants.DEFAULT_CATEGORY_NAME)).thenReturn(Optional.of(misc));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(saved);

        InventoryContracts.ProductSummary response = inventoryService.createProduct(userId, firmId, spec);

        assertEquals("Laptop", response.name());
        assertEquals("SKU-1", response.sku());
        assertEquals(10, response.currentQuantity());
        verify(notificationService).notifyProductCreatedAfterCommit(firmId, saved.getId(), "Laptop", "SKU-1", 10);
    }

    @Test
    void setStockWhenProductMissingThrowsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndFirmIdForUpdate(productId, firmId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.setStock(userId, firmId, productId, 5)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void adjustStockBelowZeroThrowsBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity product = new ProductEntity(firmId, "Mouse", "SKU-2", 1, true, null, misc, null, null);

        when(productRepository.findByIdAndFirmIdForUpdate(productId, firmId)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.adjustStock(userId, firmId, productId, -2)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void setStockCreatesAuditEvent() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity product = new ProductEntity(firmId, "Keyboard", "KB-1", 10, true, null, misc, null, null);

        when(productRepository.findByIdAndFirmIdForUpdate(productId, firmId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);

        inventoryService.setStock(userId, firmId, productId, 20);

        verify(stockChangeEventRepository).save(any());
    }

    @Test
    void adjustStockCreatesAuditEvent() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity product = new ProductEntity(firmId, "Mouse", "MS-1", 10, true, null, misc, null, null);

        when(productRepository.findByIdAndFirmIdForUpdate(productId, firmId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);

        inventoryService.adjustStock(userId, firmId, productId, -3);

        verify(stockChangeEventRepository).save(any());
    }

    @Test
    void adjustStockCrossingBelowThresholdPublishesLowStockNotification() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity product = new ProductEntity(firmId, "Mouse", "MS-LOW", 5, true, 4, misc, null, null);
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findByIdAndFirmIdForUpdate(productId, firmId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);

        inventoryService.adjustStock(userId, firmId, productId, -2);

        verify(notificationService).notifyProductLowStockAfterCommit(firmId, productId, "Mouse", "MS-LOW", 3, 4);
    }

    @Test
    void adjustStockWhileAlreadyBelowThresholdDoesNotPublishAnotherLowStockNotification() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity product = new ProductEntity(firmId, "Mouse", "MS-LOW", 3, true, 4, misc, null, null);
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findByIdAndFirmIdForUpdate(productId, firmId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);

        inventoryService.adjustStock(userId, firmId, productId, -1);

        verify(notificationService, never()).notifyProductLowStockAfterCommit(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void listBuyListReturnsProductsBelowThreshold() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CategoryEntity misc = new CategoryEntity(firmId, "Misc");
        ProductEntity low = new ProductEntity(firmId, "Low", "L-1", 2, true, null, misc, null, null);
        when(productRepository.findNeedingRestock(firmId, 4)).thenReturn(List.of(low));

        List<InventoryContracts.BuyListLine> list = inventoryService.listBuyList(userId, firmId);

        assertEquals(1, list.size());
        assertEquals(4, list.getFirst().effectiveMinThreshold());
        assertEquals(2, list.getFirst().shortfall());
        assertEquals(2, list.getFirst().currentQuantity());
    }

    @Test
    void effectiveMinThresholdUsesProductOverride() {
        CategoryEntity c = new CategoryEntity(UUID.randomUUID(), "Misc");
        ProductEntity p = new ProductEntity(UUID.randomUUID(), "P", "S", 0, true, 10, c, null, null);
        assertEquals(10, inventoryService.effectiveMinThreshold(p));
    }

    @Test
    void effectiveMinThresholdUsesDefaultWhenNull() {
        CategoryEntity c = new CategoryEntity(UUID.randomUUID(), "Misc");
        ProductEntity p = new ProductEntity(UUID.randomUUID(), "P", "S", 0, true, null, c, null, null);
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
                        new InventoryContracts.UpdateProductSpec(null, null, null, null, null, null, null, null))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
