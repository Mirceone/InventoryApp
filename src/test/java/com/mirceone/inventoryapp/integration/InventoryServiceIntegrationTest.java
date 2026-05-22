package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.inventory.InventoryService;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
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
        authService.signup(new AuthContracts.SignupSpec("inventory-it@example.com", "password123", "Inventory IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("inventory-it@example.com").orElse(null);
        assertNotNull(user);

        UUID userId = user.getId();
        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm IT"));

        InventoryContracts.ProductSummary created = inventoryService.createProduct(
                userId,
                firm.id(),
                new InventoryContracts.CreateProductSpec("Keyboard", "KB-IT", 10, null, null, null, null, null)
        );

        inventoryService.setStock(userId, firm.id(), created.id(), 25);
        inventoryService.adjustStock(userId, firm.id(), created.id(), -3);

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
        authService.signup(new AuthContracts.SignupSpec("buylist-it@example.com", "password123", "Buy List IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("buylist-it@example.com").orElse(null);
        assertNotNull(user);

        UUID userId = user.getId();
        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm BuyList"));

        InventoryContracts.ProductSummary okStock = inventoryService.createProduct(
                userId,
                firm.id(),
                new InventoryContracts.CreateProductSpec("InStock", "OK-1", 10, true, null, null, null, null)
        );
        InventoryContracts.ProductSummary lowStock = inventoryService.createProduct(
                userId,
                firm.id(),
                new InventoryContracts.CreateProductSpec("Low", "LOW-1", 2, true, null, null, null, null)
        );
        inventoryService.createProduct(
                userId,
                firm.id(),
                new InventoryContracts.CreateProductSpec("Disabled", "OFF-1", 1, false, null, null, null, null)
        );

        List<InventoryContracts.BuyListLine> buyList = inventoryService.listBuyList(userId, firm.id());

        assertEquals(1, buyList.size());
        assertEquals(lowStock.id(), buyList.getFirst().id());
        assertTrue(buyList.stream().noneMatch(i -> i.id().equals(okStock.id())));
    }
}
