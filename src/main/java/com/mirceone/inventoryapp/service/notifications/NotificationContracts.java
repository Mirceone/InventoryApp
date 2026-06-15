package com.mirceone.inventoryapp.service.notifications;

import com.mirceone.inventoryapp.model.NotificationLevel;
import com.mirceone.inventoryapp.model.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NotificationContracts {

    private NotificationContracts() {
    }

    public record NotificationSummary(
            UUID id,
            UUID firmId,
            NotificationType type,
            NotificationLevel level,
            String title,
            String body,
            Map<String, String> metadata,
            boolean read,
            Instant readAt,
            Instant createdAt
    ) {
    }

    public record NotificationInbox(
            long unreadCount,
            List<NotificationSummary> items
    ) {
    }
}
