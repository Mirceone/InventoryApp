package com.mirceone.inventoryapp.service.inventory;

import com.mirceone.inventoryapp.model.CategoryConstants;
import com.mirceone.inventoryapp.model.CategoryEntity;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.model.StockChangeEventEntity;
import com.mirceone.inventoryapp.model.StockChangeType;
import com.mirceone.inventoryapp.repository.CategoryRepository;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.repository.RouteStopRepository;
import com.mirceone.inventoryapp.repository.StockChangeEventRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockChangeEventRepository stockChangeEventRepository;
    private final CategoryRepository categoryRepository;
    private final FirmAccessService firmAccessService;
    private final RouteStopRepository routeStopRepository;
    private final NotificationService notificationService;
    private final int defaultReorderThreshold;

    public InventoryService(
            ProductRepository productRepository,
            StockChangeEventRepository stockChangeEventRepository,
            CategoryRepository categoryRepository,
            FirmAccessService firmAccessService,
            RouteStopRepository routeStopRepository,
            NotificationService notificationService,
            @Value("${app.inventory.default-reorder-threshold:4}") int defaultReorderThreshold
    ) {
        this.productRepository = productRepository;
        this.stockChangeEventRepository = stockChangeEventRepository;
        this.categoryRepository = categoryRepository;
        this.firmAccessService = firmAccessService;
        this.routeStopRepository = routeStopRepository;
        this.notificationService = notificationService;
        this.defaultReorderThreshold = defaultReorderThreshold;
    }

    public InventoryContracts.ProductSummary createProduct(UUID userId, UUID firmId, InventoryContracts.CreateProductSpec request) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.INVENTORY_WRITE);

        boolean reorderEnabled = request.reorderEnabled() != null ? request.reorderEnabled() : true;
        CategoryEntity category = resolveCategory(firmId, request.categoryId());
        UUID preferredStop = resolvePreferredStopId(firmId, request.preferredRouteStopId());
        ProductEntity product = new ProductEntity(
                firmId,
                request.name(),
                request.sku(),
                request.initialQuantity(),
                reorderEnabled,
                request.reorderThreshold(),
                category,
                normalizeImgUrl(request.imgUrl()),
                preferredStop
        );
        product = productRepository.save(product);
        notificationService.notifyProductCreatedAfterCommit(
                firmId,
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity()
        );

        return toResponse(product);
    }

    public InventoryContracts.ProductSummary updateProduct(UUID userId, UUID firmId, UUID productId, InventoryContracts.UpdateProductSpec request) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.INVENTORY_WRITE);
        if (request.name() == null && request.sku() == null && request.reorderEnabled() == null && request.reorderThreshold() == null
                && request.categoryId() == null && request.imgUrl() == null
                && request.preferredRouteStopId() == null && request.clearPreferredRouteStop() == null) {
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
        if (request.categoryId() != null) {
            product.setCategory(resolveCategory(firmId, request.categoryId()));
        }
        if (request.imgUrl() != null) {
            product.setImgUrl(normalizeImgUrl(request.imgUrl()));
        }
        if (Boolean.TRUE.equals(request.clearPreferredRouteStop())) {
            product.setPreferredRouteStopId(null);
        } else if (request.preferredRouteStopId() != null) {
            product.setPreferredRouteStopId(resolvePreferredStopId(firmId, request.preferredRouteStopId()));
        }

        product = productRepository.save(product);
        return toResponse(product);
    }

    public List<InventoryContracts.BuyListLine> listBuyList(UUID userId, UUID firmId) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.INVENTORY_WRITE);

        return productRepository.findNeedingRestock(firmId, defaultReorderThreshold).stream()
                .map(this::toBuyListItem)
                .toList();
    }

    public List<InventoryContracts.ProductSummary> listProducts(UUID userId, UUID firmId) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.INVENTORY_WRITE);

        return productRepository.findAllByFirmId(firmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InventoryContracts.ProductSummary setStock(UUID userId, UUID firmId, UUID productId, int quantity) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.INVENTORY_WRITE);

        ProductEntity product = productRepository.findByIdAndFirmId(productId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int previousQuantity = product.getCurrentQuantity();
        int newQuantity = quantity;
        product.setCurrentQuantity(quantity);
        product = productRepository.save(product);
        saveStockEvent(userId, firmId, product.getId(), StockChangeType.SET, previousQuantity, newQuantity);
        notifyIfLowStockCrossed(firmId, product, previousQuantity, newQuantity);

        return toResponse(product);
    }

    public InventoryContracts.ProductSummary adjustStock(UUID userId, UUID firmId, UUID productId, int delta) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.INVENTORY_WRITE);

        ProductEntity product = productRepository.findByIdAndFirmId(productId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int previousQuantity = product.getCurrentQuantity();
        int newQuantity = previousQuantity + delta;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        product.setCurrentQuantity(newQuantity);
        product = productRepository.save(product);
        saveStockEvent(userId, firmId, product.getId(), StockChangeType.ADJUST, previousQuantity, newQuantity);
        notifyIfLowStockCrossed(firmId, product, previousQuantity, newQuantity);

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

    private void notifyIfLowStockCrossed(UUID firmId, ProductEntity product, int previousQuantity, int newQuantity) {
        if (!product.isReorderEnabled()) {
            return;
        }
        int threshold = effectiveMinThreshold(product);
        boolean wasLow = previousQuantity < threshold;
        boolean isLow = newQuantity < threshold;
        if (!wasLow && isLow) {
            notificationService.notifyProductLowStockAfterCommit(
                    firmId,
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    newQuantity,
                    threshold
            );
        }
    }

    private InventoryContracts.ProductSummary toResponse(ProductEntity product) {
        CategoryEntity c = product.getCategory();
        return new InventoryContracts.ProductSummary(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity(),
                product.isReorderEnabled(),
                product.getReorderThreshold(),
                c.getId(),
                c.getName(),
                product.getImgUrl(),
                product.getPreferredRouteStopId()
        );
    }

    private InventoryContracts.BuyListLine toBuyListItem(ProductEntity product) {
        int effective = effectiveMinThreshold(product);
        int shortfall = effective - product.getCurrentQuantity();
        CategoryEntity c = product.getCategory();
        return new InventoryContracts.BuyListLine(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity(),
                effective,
                shortfall,
                c.getId(),
                c.getName(),
                product.getPreferredRouteStopId()
        );
    }

    private UUID resolvePreferredStopId(UUID firmId, UUID stopId) {
        if (stopId == null) {
            return null;
        }
        routeStopRepository.findByIdAndFirmId(stopId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route stop not found"));
        return stopId;
    }

    private CategoryEntity resolveCategory(UUID firmId, UUID categoryId) {
        if (categoryId == null) {
            return categoryRepository.findByFirmIdAndName(firmId, CategoryConstants.DEFAULT_CATEGORY_NAME)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Default category not found for firm"
                    ));
        }
        return categoryRepository.findByIdAndFirmId(categoryId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private static String normalizeImgUrl(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) {
            return null;
        }
        return imgUrl.strip();
    }

    /** Visible for tests in the same package ({@link com.mirceone.inventoryapp.service.inventory}). */
    int effectiveMinThreshold(ProductEntity product) {
        return product.getReorderThreshold() != null ? product.getReorderThreshold() : defaultReorderThreshold;
    }
}
