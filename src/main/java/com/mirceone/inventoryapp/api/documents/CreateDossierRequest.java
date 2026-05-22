package com.mirceone.inventoryapp.api.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDossierRequest(
        @NotBlank @Size(max = 255) String name
) {
}
