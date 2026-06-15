package com.mirceone.inventoryapp.api.firms.dashboard;

import com.mirceone.inventoryapp.api.workorders.WorkOrderResponse;
import com.mirceone.inventoryapp.api.firms.FirmResponse;
import com.mirceone.inventoryapp.api.inventory.BuyListItemResponse;
import com.mirceone.inventoryapp.api.notifications.NotificationResponse;

import java.util.List;

public record FirmDashboardResponse(
        FirmResponse firm,
        InventorySection inventory,
        WorkOrdersSection workOrders,
        ActivitySection activity,
        TeamSection team
) {
    public record InventorySection(
            long productCount,
            long categoryCount,
            long lowStockCount,
            List<BuyListItemResponse> topLowStockItems
    ) {
    }

    public record WorkOrdersSection(
            long totalWorkOrders,
            long totalFiles,
            List<WorkOrderResponse> recentWorkOrders
    ) {
    }

    public record ActivitySection(
            long unreadCount,
            List<NotificationResponse> recentNotifications
    ) {
    }

    public record TeamSection(
            long memberCount,
            long pendingInvitationCount
    ) {
    }
}
