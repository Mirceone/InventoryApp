package com.mirceone.inventoryapp.api.firms.members;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ConfirmOwnershipTransferRequest(
        UUID newOwnerUserId,
        @NotBlank @Pattern(regexp = "\\d{6}") String confirmationCode
) {
}
