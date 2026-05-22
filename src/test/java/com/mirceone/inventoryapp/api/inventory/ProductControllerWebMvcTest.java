package com.mirceone.inventoryapp.api.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.inventory.InventoryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
class ProductControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void createProductReturnsCreatedProduct() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Keyboard", "KB-001", 5, null, null, null, null, null);
        InventoryContracts.ProductSummary svcResult = new InventoryContracts.ProductSummary(
                UUID.randomUUID(), "Keyboard", "KB-001", 5, true, null, categoryId, "Misc", null, null);

        when(inventoryService.createProduct(eq(userId), eq(firmId), any(InventoryContracts.CreateProductSpec.class)))
                .thenReturn(svcResult);

        mockMvc.perform(post("/firms/{firmId}/products", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.sku").value("KB-001"))
                .andExpect(jsonPath("$.currentQuantity").value(5));
    }

    @Test
    void listProductsReturnsProductsForFirm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        when(inventoryService.listProducts(eq(userId), eq(firmId))).thenReturn(List.of(
                new InventoryContracts.ProductSummary(UUID.randomUUID(), "Mouse", "MS-1", 10, true, null, c1, "Misc", null, null),
                new InventoryContracts.ProductSummary(UUID.randomUUID(), "Monitor", "MN-1", 4, true, 6, c2, "Electronics", null, null)
        ));

        mockMvc.perform(get("/firms/{firmId}/products", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mouse"))
                .andExpect(jsonPath("$[1].name").value("Monitor"));
    }

    @Test
    void setStockReturnsUpdatedProduct() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SetStockRequest request = new SetStockRequest(25);
        InventoryContracts.ProductSummary svcResult = new InventoryContracts.ProductSummary(
                productId, "Monitor", "MN-1", 25, true, null, UUID.randomUUID(), "Misc", null, null);

        when(inventoryService.setStock(eq(userId), eq(firmId), eq(productId), eq(25))).thenReturn(svcResult);

        mockMvc.perform(put("/firms/{firmId}/products/{productId}/stock", firmId, productId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuantity").value(25));
    }

    @Test
    void adjustStockReturnsUpdatedProduct() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AdjustStockRequest request = new AdjustStockRequest(-3);
        InventoryContracts.ProductSummary svcResult = new InventoryContracts.ProductSummary(
                productId, "Mouse", "MS-1", 7, true, null, UUID.randomUUID(), "Misc", null, null);

        when(inventoryService.adjustStock(eq(userId), eq(firmId), eq(productId), eq(-3))).thenReturn(svcResult);

        mockMvc.perform(post("/firms/{firmId}/products/{productId}/stock/adjust", firmId, productId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuantity").value(7));
    }

    @Test
    void createProductWithInvalidPayloadReturnsValidationError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateProductRequest invalid = new CreateProductRequest("", "SKU", 1, null, null, null, null, null);

        mockMvc.perform(post("/firms/{firmId}/products", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void adjustStockWhenBusinessRuleFailsReturnsBusinessError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AdjustStockRequest request = new AdjustStockRequest(-999);

        when(inventoryService.adjustStock(eq(userId), eq(firmId), eq(productId), eq(-999)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative"));

        mockMvc.perform(post("/firms/{firmId}/products/{productId}/stock/adjust", firmId, productId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Stock cannot be negative"));
    }

    @Test
    void listBuyListReturnsRestockItems() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        when(inventoryService.listBuyList(eq(userId), eq(firmId))).thenReturn(List.of(
                new InventoryContracts.BuyListLine(productId, "Low", "L-1", 2, 4, 2, catId, "Misc", null)
        ));

        mockMvc.perform(get("/firms/{firmId}/products/buy-list", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Low"))
                .andExpect(jsonPath("$[0].shortfall").value(2))
                .andExpect(jsonPath("$[0].effectiveMinThreshold").value(4));
    }

    @Test
    void updateProductReturnsUpdatedProduct() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(null, null, null, 8, null, null, null, null);
        InventoryContracts.ProductSummary svcResult = new InventoryContracts.ProductSummary(
                productId, "Name", "SKU", 5, true, 8, UUID.randomUUID(), "Misc", null, null);

        when(inventoryService.updateProduct(eq(userId), eq(firmId), eq(productId), any(InventoryContracts.UpdateProductSpec.class)))
                .thenReturn(svcResult);

        mockMvc.perform(patch("/firms/{firmId}/products/{productId}", firmId, productId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reorderThreshold").value(8));
    }

    @Test
    void listProductsWithoutTokenReturnsUnauthorized() throws Exception {
        UUID firmId = UUID.randomUUID();

        mockMvc.perform(get("/firms/{firmId}/products", firmId))
                .andExpect(status().isUnauthorized());
    }
}
