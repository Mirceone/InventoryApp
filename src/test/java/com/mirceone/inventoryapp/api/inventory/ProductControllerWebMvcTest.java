package com.mirceone.inventoryapp.api.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.InventoryService;
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
    private AuthRateLimiter authRateLimiter;

    @Test
    void createProductReturnsCreatedProduct() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Keyboard", "KB-001", 5, null, null);
        ProductResponse response = new ProductResponse(UUID.randomUUID(), "Keyboard", "KB-001", 5, true, null);

        when(inventoryService.createProduct(eq(userId), eq(firmId), any(CreateProductRequest.class))).thenReturn(response);

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
        when(inventoryService.listProducts(eq(userId), eq(firmId))).thenReturn(List.of(
                new ProductResponse(UUID.randomUUID(), "Mouse", "MS-1", 10, true, null),
                new ProductResponse(UUID.randomUUID(), "Monitor", "MN-1", 4, true, 6)
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
        ProductResponse response = new ProductResponse(productId, "Monitor", "MN-1", 25, true, null);

        when(inventoryService.setStock(eq(userId), eq(firmId), eq(productId), any(SetStockRequest.class))).thenReturn(response);

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
        ProductResponse response = new ProductResponse(productId, "Mouse", "MS-1", 7, true, null);

        when(inventoryService.adjustStock(eq(userId), eq(firmId), eq(productId), any(AdjustStockRequest.class))).thenReturn(response);

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
        CreateProductRequest invalid = new CreateProductRequest("", "SKU", 1, null, null);

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

        when(inventoryService.adjustStock(eq(userId), eq(firmId), eq(productId), any(AdjustStockRequest.class)))
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
        when(inventoryService.listBuyList(eq(userId), eq(firmId))).thenReturn(List.of(
                new BuyListItemResponse(productId, "Low", "L-1", 2, 4, 2)
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
        UpdateProductRequest request = new UpdateProductRequest(null, null, null, 8);
        ProductResponse response = new ProductResponse(productId, "Name", "SKU", 5, true, 8);

        when(inventoryService.updateProduct(eq(userId), eq(firmId), eq(productId), any(UpdateProductRequest.class)))
                .thenReturn(response);

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
