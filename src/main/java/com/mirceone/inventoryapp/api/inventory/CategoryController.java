package com.mirceone.inventoryapp.api.inventory;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.inventory.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/categories")
@Tag(name = "Categories", description = "Product categories per firm")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @Operation(summary = "Create category for firm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation or reserved category name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member"),
            @ApiResponse(responseCode = "409", description = "Category already exists")
    })
    public CategoryResponse createCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return InventoryWebMapper.toCategoryResponse(categoryService.createCategory(userId, firmId, request.name()));
    }

    @PatchMapping("/{categoryId}")
    @Operation(summary = "Rename category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "400", description = "Validation or reserved category"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Category already exists")
    })
    public CategoryResponse updateCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return InventoryWebMapper.toCategoryResponse(
                categoryService.updateCategory(userId, firmId, categoryId, request.name())
        );
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete category and move its products to Misc")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "400", description = "Reserved category"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public void deleteCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID categoryId
    ) {
        categoryService.deleteCategory(userId, firmId, categoryId);
    }

    @GetMapping
    @Operation(summary = "List categories for firm (includes default Misc)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm member")
    })
    public List<CategoryResponse> listCategories(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId
    ) {
        return InventoryWebMapper.toCategoryResponseList(categoryService.listCategories(userId, firmId));
    }
}
