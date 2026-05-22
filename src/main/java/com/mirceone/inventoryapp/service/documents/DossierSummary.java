package com.mirceone.inventoryapp.service.documents;

import java.time.Instant;
import java.util.UUID;

public record DossierSummary(
        UUID id,
        UUID firmId,
        String name,
        UUID createdByUserId,
        Instant createdAt,
        long documentCount
) {
}
