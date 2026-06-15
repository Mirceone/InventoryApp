package com.mirceone.inventoryapp.api.firms;

import com.mirceone.inventoryapp.service.firms.FirmContracts;

import java.util.List;

public final class FirmWebMapper {

    private FirmWebMapper() {
    }

    public static FirmContracts.CreateFirmSpec toCreateSpec(CreateFirmRequest request) {
        return new FirmContracts.CreateFirmSpec(request.name());
    }

    public static FirmContracts.UpdateFirmSpec toUpdateSpec(UpdateFirmRequest request) {
        return new FirmContracts.UpdateFirmSpec(request.name());
    }

    public static FirmContracts.UpdateFirmStatusSpec toUpdateStatusSpec(UpdateFirmStatusRequest request) {
        return new FirmContracts.UpdateFirmStatusSpec(request.status(), request.message());
    }

    public static FirmResponse toResponse(FirmContracts.FirmSummary firm) {
        return new FirmResponse(
                firm.id(),
                firm.name(),
                firm.role().name(),
                firm.roleDisplayLabel(),
                firm.status().name(),
                firm.statusDisplayLabel(),
                firm.statusMessage()
        );
    }

    public static List<FirmResponse> toResponseList(List<FirmContracts.FirmSummary> firms) {
        return firms.stream().map(FirmWebMapper::toResponse).toList();
    }

    public static FirmStatusHistoryResponse toHistoryResponse(FirmContracts.FirmStatusHistoryEntry entry) {
        return new FirmStatusHistoryResponse(
                entry.id(),
                entry.previousStatus() == null ? null : entry.previousStatus().name(),
                entry.previousStatusDisplayLabel(),
                entry.newStatus().name(),
                entry.newStatusDisplayLabel(),
                entry.message(),
                entry.actorUserId(),
                entry.source().name(),
                entry.createdAt()
        );
    }

    public static List<FirmStatusHistoryResponse> toHistoryResponseList(List<FirmContracts.FirmStatusHistoryEntry> history) {
        return history.stream().map(FirmWebMapper::toHistoryResponse).toList();
    }
}
