package com.mirceone.inventoryapp.service.auth;

import com.mirceone.inventoryapp.model.ProviderType;

import java.util.UUID;

public final class AuthContracts {

    private AuthContracts() {
    }

    public record SignupSpec(String email, String password, String displayName) {
    }

    public record LoginSpec(String email, String password) {
    }

    public record RefreshSpec(String refreshToken) {
    }

    public record LogoutSpec(String refreshToken) {
    }

    public record IssuedTokenPair(
            String tokenType,
            String accessToken,
            long expiresInSeconds,
            String refreshToken,
            long refreshExpiresInSeconds
    ) {
    }

    public record CurrentUserSnapshot(UUID id, String email, String displayName, ProviderType provider) {
    }

    public record ForgotPasswordSpec(String email) {
    }

    public record CompletePasswordResetSpec(String token, String newPassword) {
    }
}
