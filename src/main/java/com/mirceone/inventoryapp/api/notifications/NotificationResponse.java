package com.mirceone.inventoryapp.api.notifications;

import com.mirceone.inventoryapp.model.NotificationLevel;
import com.mirceone.inventoryapp.model.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
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
