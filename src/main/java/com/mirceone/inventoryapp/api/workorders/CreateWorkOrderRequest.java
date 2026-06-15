package com.mirceone.inventoryapp.api.workorders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWorkOrderRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String clientName,
        @NotBlank @Size(max = 255) String location,
        @Size(max = 2000) String description,
        @NotNull LocalDate estimatedEndDate
) {
}
