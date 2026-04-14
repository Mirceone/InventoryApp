package com.mirceone.inventoryapp.api.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
class CategoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private AuthRateLimiter authRateLimiter;

    @Test
    void createCategoryReturnsCreatedCategory() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CreateCategoryRequest request = new CreateCategoryRequest("Tools");

        when(categoryService.createCategory(eq(userId), eq(firmId), eq("Tools")))
                .thenReturn(new CategoryResponse(categoryId, "Tools"));

        mockMvc.perform(post("/firms/{firmId}/categories", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Tools"));
    }

    @Test
    void createCategoryWithDuplicateNameReturnsBusinessError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateCategoryRequest request = new CreateCategoryRequest("Tools");

        when(categoryService.createCategory(eq(userId), eq(firmId), any(String.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists"));

        mockMvc.perform(post("/firms/{firmId}/categories", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Category already exists"));
    }

    @Test
    void createCategoryWithInvalidPayloadReturnsValidationError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateCategoryRequest request = new CreateCategoryRequest("  ");

        mockMvc.perform(post("/firms/{firmId}/categories", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateCategoryReturnsUpdatedCategory() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Hardware");

        when(categoryService.updateCategory(eq(userId), eq(firmId), eq(categoryId), eq("Hardware")))
                .thenReturn(new CategoryResponse(categoryId, "Hardware"));

        mockMvc.perform(patch("/firms/{firmId}/categories/{categoryId}", firmId, categoryId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hardware"));
    }

    @Test
    void deleteCategoryReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        mockMvc.perform(delete("/firms/{firmId}/categories/{categoryId}", firmId, categoryId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMiscCategoryReturnsBusinessError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category 'Misc' is managed by the system"))
                .when(categoryService)
                .deleteCategory(eq(userId), eq(firmId), eq(categoryId));

        mockMvc.perform(delete("/firms/{firmId}/categories/{categoryId}", firmId, categoryId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void listCategoriesReturnsMiscAndOthers() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID miscId = UUID.randomUUID();
        when(categoryService.listCategories(eq(userId), eq(firmId))).thenReturn(List.of(
                new CategoryResponse(miscId, "Misc"),
                new CategoryResponse(UUID.randomUUID(), "Tools")
        ));

        mockMvc.perform(get("/firms/{firmId}/categories", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Misc"))
                .andExpect(jsonPath("$[1].name").value("Tools"));
    }
}

