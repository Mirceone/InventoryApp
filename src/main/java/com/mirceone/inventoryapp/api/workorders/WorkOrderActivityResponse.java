package com.mirceone.inventoryapp.api.workorders;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderActivityResponse(
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
