package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.NotificationType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.inventory.InventoryService;
import com.mirceone.inventoryapp.service.notifications.NotificationContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class NotificationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private NotificationService notificationService;

    @Test
    void lowStockCrossingAddsProductToBuyListAndCreatesNotification() {
        authService.signup(new AuthContracts.SignupSpec("notify-owner@example.com", "password123", "Owner"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("notify-owner@example.com").orElseThrow();
        UUID userId = owner.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Notify Firm"));

        InventoryContracts.ProductSummary product = inventoryService.createProduct(
                userId,
                firm.id(),
                new InventoryContracts.CreateProductSpec("Laptop", "LT-1", 10, true, 4, null, null, null)
        );

        InventoryContracts.ProductSummary updated = inventoryService.adjustStock(
                userId,
                firm.id(),
                product.id(),
                -7
        );
        assertEquals(3, updated.currentQuantity());

        var buyList = inventoryService.listBuyList(userId, firm.id());
        assertEquals(1, buyList.size());
        assertEquals(product.id(), buyList.getFirst().id());
        assertEquals(4, buyList.getFirst().effectiveMinThreshold());

        NotificationContracts.NotificationInbox inbox = notificationService.listNotifications(userId, false, 20);
        assertEquals(2, inbox.unreadCount());
        Set<NotificationType> types = inbox.items().stream().map(NotificationContracts.NotificationSummary::type).collect(java.util.stream.Collectors.toSet());
        assertTrue(types.contains(NotificationType.PRODUCT_CREATED));
        assertTrue(types.contains(NotificationType.PRODUCT_LOW_STOCK));
    }

    @Test
    void statusChangeCreatesInAppNotification() {
        authService.signup(new AuthContracts.SignupSpec("notify-status@example.com", "password123", "Owner"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("notify-status@example.com").orElseThrow();
        UUID userId = owner.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Status Notify Firm"));

        firmService.updateFirmStatus(
                userId,
                firm.id(),
                new FirmContracts.UpdateFirmStatusSpec(FirmStatus.PAUSED, "Manual pause")
        );

        NotificationContracts.NotificationInbox inbox = notificationService.listNotifications(userId, false, 20);
        assertEquals(1, inbox.unreadCount());
        assertEquals(NotificationType.FIRM_STATUS_CHANGED, inbox.items().getFirst().type());
        assertEquals("PAUSED", inbox.items().getFirst().metadata().get("newStatus"));
    }
}
