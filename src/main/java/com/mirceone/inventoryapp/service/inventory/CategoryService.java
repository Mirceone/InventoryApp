package com.mirceone.inventoryapp.service.inventory;

import com.mirceone.inventoryapp.model.CategoryConstants;
import com.mirceone.inventoryapp.model.CategoryEntity;
import com.mirceone.inventoryapp.repository.CategoryRepository;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.mirceone.inventoryapp.model.CategoryConstants.DEFAULT_CATEGORY_NAME;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FirmAccessService firmAccessService;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            FirmAccessService firmAccessService
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.firmAccessService = firmAccessService;
    }

    public InventoryContracts.CategorySummary createCategory(UUID userId, UUID firmId, String rawName) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.INVENTORY_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);

        String name = normalizeName(rawName);
        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system");
        }

        try {
            CategoryEntity saved = categoryRepository.save(new CategoryEntity(firmId, name));
            return new InventoryContracts.CategorySummary(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists", ex);
        }
    }

    /**
     * Ensures the default {@value CategoryConstants#DEFAULT_CATEGORY_NAME} category exists for a new firm.
     */
    public void ensureDefaultCategoryForFirm(UUID firmId) {
        if (categoryRepository.findByFirmIdAndName(firmId, CategoryConstants.DEFAULT_CATEGORY_NAME).isEmpty()) {
            categoryRepository.save(new CategoryEntity(firmId, CategoryConstants.DEFAULT_CATEGORY_NAME));
        }
    }

    public InventoryContracts.CategorySummary updateCategory(UUID userId, UUID firmId, UUID categoryId, String rawName) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.INVENTORY_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);

        CategoryEntity category = categoryRepository.findByIdAndFirmId(categoryId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(category.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system");
        }

        String name = normalizeName(rawName);
        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system");
        }

        try {
            category.setName(name);
            CategoryEntity saved = categoryRepository.save(category);
            return new InventoryContracts.CategorySummary(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists", ex);
        }
    }

    public void deleteCategory(UUID userId, UUID firmId, UUID categoryId) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.INVENTORY_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);

        CategoryEntity category = categoryRepository.findByIdAndFirmId(categoryId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(category.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system");
        }

        CategoryEntity misc = getDefaultCategoryOrThrow(firmId);
        productRepository.moveProductsToCategory(firmId, category, misc);
        categoryRepository.deleteByIdAndFirmId(categoryId, firmId);
    }

    public List<InventoryContracts.CategorySummary> listCategories(UUID userId, UUID firmId) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.INVENTORY_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        return categoryRepository.findAllByFirmIdOrderByNameAsc(firmId).stream()
                .map(c -> new InventoryContracts.CategorySummary(c.getId(), c.getName()))
                .toList();
    }

    private CategoryEntity getDefaultCategoryOrThrow(UUID firmId) {
        return categoryRepository.findByFirmIdAndName(firmId, DEFAULT_CATEGORY_NAME)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Default category not found for firm"
                ));
    }

    private static String normalizeName(String rawName) {
        return rawName == null ? "" : rawName.trim();
    }
}
