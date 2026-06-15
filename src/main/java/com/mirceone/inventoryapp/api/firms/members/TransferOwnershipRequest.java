package com.mirceone.inventoryapp.api.firms.members;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferOwnershipRequest(
        @NotNull UUID newOwnerUserId
) {
}
