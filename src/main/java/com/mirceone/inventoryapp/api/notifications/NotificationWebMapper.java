package com.mirceone.inventoryapp.api.notifications;

import com.mirceone.inventoryapp.service.notifications.NotificationContracts;

public final class NotificationWebMapper {

    private NotificationWebMapper() {
    }

    public static NotificationInboxResponse toInboxResponse(NotificationContracts.NotificationInbox inbox) {
        return new NotificationInboxResponse(
                inbox.unreadCount(),
                inbox.items().stream().map(NotificationWebMapper::toResponse).toList()
        );
    }

    public static NotificationResponse toResponse(NotificationContracts.NotificationSummary notification) {
        return new NotificationResponse(
                notification.id(),
                notification.firmId(),
                notification.type(),
                notification.level(),
                notification.title(),
                notification.body(),
                notification.metadata(),
                notification.read(),
                notification.readAt(),
                notification.createdAt()
        );
    }
}
