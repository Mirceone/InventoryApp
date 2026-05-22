package com.mirceone.inventoryapp.api.documents;

import java.time.Instant;
import java.util.UUID;

public record DossierResponse(
        UUID id,
        UUID firmId,
        String name,
        UUID createdByUserId,
        Instant createdAt,
        long documentCount
) {
}
