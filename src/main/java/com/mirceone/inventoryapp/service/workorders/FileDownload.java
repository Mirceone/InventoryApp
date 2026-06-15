package com.mirceone.inventoryapp.service.workorders;

import org.springframework.core.io.Resource;

public record FileDownload(
        String displayName,
        String mimeType,
        Resource resource
) {
}
