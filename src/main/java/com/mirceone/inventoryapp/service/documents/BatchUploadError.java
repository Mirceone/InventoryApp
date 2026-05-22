package com.mirceone.inventoryapp.service.documents;

public record BatchUploadError(
        String originalFilename,
        String message
) {
}
