package com.mirceone.inventoryapp.api.workorders;

import java.util.List;
import java.util.UUID;

public record FolderNodeResponse(
        UUID id,
        String name,
        String path,
        boolean catchAll,
        long fileCount,
        List<String> extensions,
        List<FolderNodeResponse> children
) {
}
