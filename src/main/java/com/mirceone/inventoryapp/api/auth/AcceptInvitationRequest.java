package com.mirceone.inventoryapp.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank String token,
        @Size(max = 255) String displayName,
        @Size(min = 8, max = 72) String password
) {
}
