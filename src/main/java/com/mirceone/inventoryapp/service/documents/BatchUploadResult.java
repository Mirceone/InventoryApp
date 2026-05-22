package com.mirceone.inventoryapp.service.documents;

import java.util.List;

public record BatchUploadResult(
        List<BatchUploadItem> accepted,
        List<BatchUploadError> errors
) {
}
