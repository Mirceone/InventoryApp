package com.mirceone.inventoryapp.api.firms.members;

import java.time.Instant;
import java.util.UUID;

public record FirmInvitationResponse(
        UUID id,
        String email,
        String role,
        String roleDisplayLabel,
        String status,
        Instant expiresAt,
        Instant createdAt
) {
}
