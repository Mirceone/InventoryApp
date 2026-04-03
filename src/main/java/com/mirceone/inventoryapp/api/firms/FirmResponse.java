package com.mirceone.inventoryapp.api.firms;

import java.util.UUID;

public record FirmResponse(
        UUID id,
        String name
) {
}
