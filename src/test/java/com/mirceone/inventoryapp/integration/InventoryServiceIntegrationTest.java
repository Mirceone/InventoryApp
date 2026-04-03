package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.api.firms.CreateFirmRequest;
import com.mirceone.inventoryapp.api.firms.FirmResponse;
import com.mirceone.inventoryapp.api.inventory.AdjustStockRequest;
import com.mirceone.inventoryapp.api.inventory.BuyListItemResponse;
import com.mirceone.inventoryapp.api.inventory.CreateProductRequest;
import com.mirceone.inventoryapp.api.inventory.ProductResponse;
import com.mirceone.inventoryapp.api.inventory.SetStockRequest;
import com.mirceone.inventoryapp.api.auth.SignupRequest;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.AuthService;
import com.mirceone.inventoryapp.service.FirmService;
import com.mirceone.inventoryapp.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InventoryServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void stockChangesArePersistedInAuditTrail() {
        authService.signup(new SignupRequest("inventory-it@example.com", "password123", "Inventory IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("inventory-it@example.com").orElse(null);
        assertNotNull(user);

        UUID userId = user.getId();
        FirmResponse firm = firmService.createFirm(userId, new CreateFirmRequest("Firm IT"));

        ProductResponse created = inventoryService.createProduct(
                userId,
                firm.id(),
                new CreateProductRequest("Keyboard", "KB-IT", 10, null, null)
        );

        inventoryService.setStock(userId, firm.id(), created.id(), new SetStockRequest(25));
        inventoryService.adjustStock(userId, firm.id(), created.id(), new AdjustStockRequest(-3));

        Integer eventsCount = jdbcTemplate.queryForObject(
                "select count(*) from stock_change_events where product_id = ?",
                Integer.class,
                created.id()
        );
        Integer deltaSum = jdbcTemplate.queryForObject(
                "select coalesce(sum(delta), 0) from stock_change_events where product_id = ?",
                Integer.class,
                created.id()
        );

        assertEquals(2, eventsCount);
        assertEquals(12, deltaSum);
    }

    @Test
    void buyListIncludesProductsBelowEffectiveThreshold() {
        authService.signup(new SignupRequest("buylist-it@example.com", "password123", "Buy List IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("buylist-it@example.com").orElse(null);
        assertNotNull(user);

        UUID userId = user.getId();
        FirmResponse firm = firmService.createFirm(userId, new CreateFirmRequest("Firm BuyList"));

        ProductResponse okStock = inventoryService.createProduct(
                userId,
                firm.id(),
                new CreateProductRequest("InStock", "OK-1", 10, true, null)
        );
        ProductResponse lowStock = inventoryService.createProduct(
                userId,
                firm.id(),
                new CreateProductRequest("Low", "LOW-1", 2, true, null)
        );
        inventoryService.createProduct(
                userId,
                firm.id(),
                new CreateProductRequest("Disabled", "OFF-1", 1, false, null)
        );

        List<BuyListItemResponse> buyList = inventoryService.listBuyList(userId, firm.id());

        assertEquals(1, buyList.size());
        assertEquals(lowStock.id(), buyList.getFirst().id());
        assertTrue(buyList.stream().noneMatch(i -> i.id().equals(okStock.id())));
    }
}
