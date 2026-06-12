package com.mirceone.inventoryapp.api.workorders;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceFolderRulesRequest(
        @NotNull List<String> extensions
) {
}
