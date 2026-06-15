package com.mirceone.inventoryapp.api.firms.dashboard;

import com.mirceone.inventoryapp.api.workorders.WorkOrderWebMapper;
import com.mirceone.inventoryapp.api.firms.FirmWebMapper;
import com.mirceone.inventoryapp.api.inventory.InventoryWebMapper;
import com.mirceone.inventoryapp.api.notifications.NotificationWebMapper;
import com.mirceone.inventoryapp.service.firms.dashboard.FirmDashboardContracts;

public final class FirmDashboardWebMapper {

    private FirmDashboardWebMapper() {
    }

    public static FirmDashboardResponse toResponse(FirmDashboardContracts.DashboardSnapshot snapshot) {
        return new FirmDashboardResponse(
                FirmWebMapper.toResponse(snapshot.firm()),
                new FirmDashboardResponse.InventorySection(
                        snapshot.inventory().productCount(),
                        snapshot.inventory().categoryCount(),
                        snapshot.inventory().lowStockCount(),
                        InventoryWebMapper.toBuyListResponseList(snapshot.inventory().topLowStockItems())
                ),
                new FirmDashboardResponse.WorkOrdersSection(
                        snapshot.workOrders().totalWorkOrders(),
                        snapshot.workOrders().totalFiles(),
                        snapshot.workOrders().recentWorkOrders().stream()
                                .map(WorkOrderWebMapper::toResponse)
                                .toList()
                ),
                new FirmDashboardResponse.ActivitySection(
                        snapshot.activity().unreadCount(),
                        snapshot.activity().recentNotifications().stream()
                                .map(NotificationWebMapper::toResponse)
                                .toList()
                ),
                snapshot.team() == null
                        ? null
                        : new FirmDashboardResponse.TeamSection(
                        snapshot.team().memberCount(),
                        snapshot.team().pendingInvitationCount()
                )
        );
    }
}
