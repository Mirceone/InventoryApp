package com.mirceone.inventoryapp.api.firms;

import com.mirceone.inventoryapp.model.FirmStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFirmStatusRequest(
        @NotNull FirmStatus status,
        @Size(max = 512) String message
) {
}
