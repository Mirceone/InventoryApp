package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.WorkOrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkOrderResponse(
        UUID id,
        UUID firmId,
        String name,
        String clientName,
        String location,
        String description,
        LocalDate estimatedEndDate,
        WorkOrderStatus status,
        UUID createdByUserId,
        Instant createdAt,
        long fileCount
) {
}
