package com.mirceone.inventoryapp.api.auth;

import java.time.Instant;

public record InvitationPreviewResponse(
        String firmName,
        String email,
        String maskedEmail,
        String role,
        String roleDisplayLabel,
        Instant expiresAt,
        boolean accountExists
) {
}
