package com.mirceone.inventoryapp.api.auth;

import com.mirceone.inventoryapp.model.ProviderType;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        ProviderType provider
) {
}
