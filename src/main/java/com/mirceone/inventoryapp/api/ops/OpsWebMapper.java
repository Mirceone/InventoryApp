package com.mirceone.inventoryapp.api.ops;

import com.mirceone.inventoryapp.model.OpsEventEntity;

import java.util.List;

public final class OpsWebMapper {

    private OpsWebMapper() {
    }

    public static OpsLogsResponse toLogsResponse(List<String> lines) {
        return new OpsLogsResponse(lines);
    }

    public static OpsEventResponse toEventResponse(OpsEventEntity event) {
        return new OpsEventResponse(
                event.getId(),
                event.getCreatedAt(),
                event.getModel(),
                event.getPromptExcerpt(),
                event.getResponseExcerpt()
        );
    }

    public static List<OpsEventResponse> toEventResponseList(List<OpsEventEntity> events) {
        return events.stream().map(OpsWebMapper::toEventResponse).toList();
    }
}
