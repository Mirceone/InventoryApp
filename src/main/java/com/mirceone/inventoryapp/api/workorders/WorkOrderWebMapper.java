package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;

public final class WorkOrderWebMapper {

    private WorkOrderWebMapper() {
    }

    public static WorkOrderContracts.CreateWorkOrderSpec toCreateWorkOrderSpec(CreateWorkOrderRequest request) {
        return new WorkOrderContracts.CreateWorkOrderSpec(
                request.name(),
                request.clientName(),
                request.location(),
                request.description(),
                request.estimatedEndDate()
        );
    }

    public static WorkOrderContracts.UpdateWorkOrderSpec toUpdateWorkOrderSpec(UpdateWorkOrderRequest request) {
        boolean clearDescription = request.isDescriptionPresent() && request.getDescription() == null;
        return new WorkOrderContracts.UpdateWorkOrderSpec(
                request.getName(),
                request.getClientName(),
                request.getLocation(),
                request.isDescriptionPresent() ? request.getDescription() : null,
                request.getEstimatedEndDate(),
                clearDescription
        );
    }

    public static WorkOrderResponse toResponse(WorkOrderSummary s) {
        return new WorkOrderResponse(
                s.id(),
                s.firmId(),
                s.name(),
                s.clientName(),
                s.location(),
                s.description(),
                s.estimatedEndDate(),
                s.status(),
                s.createdByUserId(),
                s.createdAt(),
                s.fileCount()
        );
    }
}
