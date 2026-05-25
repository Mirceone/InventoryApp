package com.mirceone.inventoryapp.api.firms;

import java.time.Instant;
import java.util.UUID;

public record FirmStatusHistoryResponse(
        UUID id,
        String previousStatus,
        String previousStatusDisplayLabel,
        String newStatus,
        String newStatusDisplayLabel,
        String message,
        UUID actorUserId,
        String source,
        Instant createdAt
) {
}
