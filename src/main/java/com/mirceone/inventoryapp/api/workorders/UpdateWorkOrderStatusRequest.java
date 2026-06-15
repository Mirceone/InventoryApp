package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkOrderStatusRequest(
        @NotNull WorkOrderStatus status
) {
}
