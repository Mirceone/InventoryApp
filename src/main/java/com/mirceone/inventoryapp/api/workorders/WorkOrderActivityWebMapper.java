package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.service.workorders.WorkOrderActivitySummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;

public final class WorkOrderActivityWebMapper {

    private WorkOrderActivityWebMapper() {
    }

    public static WorkOrderContracts.CreateActivitySpec toCreateActivitySpec(CreateActivityRequest request) {
        return new WorkOrderContracts.CreateActivitySpec(
                request.title(),
                request.description()
        );
    }

    public static WorkOrderActivityResponse toResponse(WorkOrderActivitySummary s) {
        return new WorkOrderActivityResponse(
                s.id(),
                s.workOrderId(),
                s.title(),
                s.description(),
                s.createdByUserId(),
                s.createdByEmail(),
                s.createdByDisplayName(),
                s.createdAt()
        );
    }
}
