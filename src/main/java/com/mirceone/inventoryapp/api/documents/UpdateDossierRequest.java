package com.mirceone.inventoryapp.api.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDossierRequest(
        @NotBlank @Size(max = 255) String name
) {
}
