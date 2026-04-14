package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.inventory.CategoryResponse;
import com.mirceone.inventoryapp.model.CategoryConstants;
import com.mirceone.inventoryapp.model.CategoryEntity;
import com.mirceone.inventoryapp.repository.CategoryRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.mirceone.inventoryapp.model.CategoryConstants.DEFAULT_CATEGORY_NAME;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FirmMemberRepository firmMemberRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            FirmMemberRepository firmMemberRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.firmMemberRepository = firmMemberRepository;
    }

    public CategoryResponse createCategory(UUID userId, UUID firmId, String rawName) {
        assertUserIsMember(firmId, userId);

        String name = normalizeName(rawName);
        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system");
        }

        try {
            CategoryEntity saved = categoryRepository.save(new CategoryEntity(firmId, name));
            return new CategoryResponse(saved.getId(), saved.getName());
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

    public CategoryResponse updateCategory(UUID userId, UUID firmId, UUID categoryId, String rawName) {
        assertUserIsMember(firmId, userId);

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
            return new CategoryResponse(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists", ex);
        }
    }

    public void deleteCategory(UUID userId, UUID firmId, UUID categoryId) {
        assertUserIsMember(firmId, userId);

        CategoryEntity category = categoryRepository.findByIdAndFirmId(categoryId, firmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(category.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system");
        }

        CategoryEntity misc = getDefaultCategoryOrThrow(firmId);
        productRepository.moveProductsToCategory(firmId, category, misc);
        categoryRepository.deleteByIdAndFirmId(categoryId, firmId);
    }

    public List<CategoryResponse> listCategories(UUID userId, UUID firmId) {
        assertUserIsMember(firmId, userId);
        return categoryRepository.findAllByFirmIdOrderByNameAsc(firmId).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    private void assertUserIsMember(UUID firmId, UUID userId) {
        if (!firmMemberRepository.existsByFirmIdAndUserId(firmId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this firm");
        }
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

