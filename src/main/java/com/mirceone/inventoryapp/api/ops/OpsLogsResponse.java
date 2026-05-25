package com.mirceone.inventoryapp.api.ops;

import java.util.List;

public record OpsLogsResponse(
        List<String> lines
) {
}
