package com.mirceone.inventoryapp.api.firms.dashboard;

import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.NotificationLevel;
import com.mirceone.inventoryapp.model.NotificationType;
import com.mirceone.inventoryapp.model.WorkOrderStatus;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.dashboard.FirmDashboardContracts;
import com.mirceone.inventoryapp.service.firms.dashboard.FirmDashboardService;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationContracts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FirmDashboardController.class)
class FirmDashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirmDashboardService firmDashboardService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void getDashboardReturnsAggregatedSections() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();

        FirmDashboardContracts.DashboardSnapshot snapshot = new FirmDashboardContracts.DashboardSnapshot(
                new FirmContracts.FirmSummary(
                        firmId,
                        "Axormi Serv",
                        MemberRole.OWNER,
                        "Admin",
                        FirmStatus.ACTIVE,
                        "Active",
                        null
                ),
                new FirmDashboardContracts.InventorySnapshot(
                        12,
                        4,
                        2,
                        List.of(new InventoryContracts.BuyListLine(
                                UUID.randomUUID(),
                                "Cable",
                                "CBL-1",
                                1,
                                5,
                                4,
                                UUID.randomUUID(),
                                "Electrical",
                                null
                        ))
                ),
                new FirmDashboardContracts.WorkOrderSnapshot(
                        3,
                        18,
                        List.of(new WorkOrderSummary(
                                workOrderId,
                                firmId,
                                "Site A",
                                "Client SRL",
                                "Bucharest",
                                "Notes",
                                LocalDate.parse("2026-08-01"),
                                WorkOrderStatus.IN_PROGRESS,
                                userId,
                                Instant.parse("2026-05-26T08:30:00Z"),
                                6
                        ))
                ),
                new FirmDashboardContracts.ActivitySnapshot(
                        1,
                        List.of(new NotificationContracts.NotificationSummary(
                                UUID.randomUUID(),
                                firmId,
                                NotificationType.PRODUCT_LOW_STOCK,
                                NotificationLevel.WARNING,
                                "Produs sub prag minim",
                                "Cable needs restock",
                                Map.of("event", "product_low_stock"),
                                false,
                                null,
                                Instant.parse("2026-05-26T09:00:00Z")
                        ))
                ),
                new FirmDashboardContracts.TeamSnapshot(5, 2)
        );

        when(firmDashboardService.getDashboard(userId, firmId)).thenReturn(snapshot);

        mockMvc.perform(get("/firms/{firmId}/dashboard", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firm.name").value("Axormi Serv"))
                .andExpect(jsonPath("$.inventory.lowStockCount").value(2))
                .andExpect(jsonPath("$.workOrders.totalFiles").value(18))
                .andExpect(jsonPath("$.workOrders.recentWorkOrders[0].id").value(workOrderId.toString()))
                .andExpect(jsonPath("$.activity.unreadCount").value(1))
                .andExpect(jsonPath("$.team.pendingInvitationCount").value(2));
    }
}
