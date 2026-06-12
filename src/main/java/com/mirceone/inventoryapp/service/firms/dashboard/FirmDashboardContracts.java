package com.mirceone.inventoryapp.service.firms.dashboard;

import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationContracts;

import java.util.List;

public final class FirmDashboardContracts {

    private FirmDashboardContracts() {
    }

    public record DashboardSnapshot(
            FirmContracts.FirmSummary firm,
            InventorySnapshot inventory,
            WorkOrderSnapshot workOrders,
            ActivitySnapshot activity,
            TeamSnapshot team
    ) {
    }

    public record InventorySnapshot(
            long productCount,
            long categoryCount,
            long lowStockCount,
            List<InventoryContracts.BuyListLine> topLowStockItems
    ) {
    }

    public record WorkOrderSnapshot(
            long totalWorkOrders,
            long totalFiles,
            List<WorkOrderSummary> recentWorkOrders
    ) {
    }

    public record ActivitySnapshot(
            long unreadCount,
            List<NotificationContracts.NotificationSummary> recentNotifications
    ) {
    }

    public record TeamSnapshot(
            long memberCount,
            long pendingInvitationCount
    ) {
    }
}
