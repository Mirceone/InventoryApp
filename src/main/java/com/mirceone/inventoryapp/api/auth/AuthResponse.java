package com.mirceone.inventoryapp.api.auth;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds
) {
}
