package com.mirceone.inventoryapp.api.notifications;

import java.util.List;

public record NotificationInboxResponse(
        long unreadCount,
        List<NotificationResponse> items
) {
}
