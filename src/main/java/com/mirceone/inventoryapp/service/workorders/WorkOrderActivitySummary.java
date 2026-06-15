package com.mirceone.inventoryapp.service.workorders;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderActivitySummary(
        UUID id,
        UUID workOrderId,
        String title,
        String description,
        UUID createdByUserId,
        String createdByEmail,
        String createdByDisplayName,
        Instant createdAt
) {
}
