package com.mirceone.inventoryapp.api.firms.members;

import java.time.Instant;
import java.util.UUID;

public record FirmMemberResponse(
        UUID userId,
        String email,
        String displayName,
        String role,
        String roleDisplayLabel,
        Instant joinedAt
) {
}
