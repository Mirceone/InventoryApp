package com.mirceone.inventoryapp.api.ops;

import java.time.Instant;
import java.util.UUID;

public record OpsEventResponse(
        UUID id,
        Instant createdAt,
        String model,
        String promptExcerpt,
        String responseExcerpt
) {
}
