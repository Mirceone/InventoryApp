package com.mirceone.inventoryapp.service.firms;

import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;

import java.util.UUID;

public final class FirmContracts {

    private FirmContracts() {
    }

    public record CreateFirmSpec(String name) {
    }

    public record UpdateFirmSpec(String name) {
    }

    public record UpdateFirmStatusSpec(FirmStatus status, String message) {
    }

    public record FirmSummary(
            UUID id,
            String name,
            MemberRole role,
            String roleDisplayLabel,
            FirmStatus status,
            String statusDisplayLabel,
            String statusMessage
    ) {
    }
}
