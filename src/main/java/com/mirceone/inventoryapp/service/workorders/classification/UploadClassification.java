package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.model.FileClassificationSource;
import com.mirceone.inventoryapp.model.FileClassificationStatus;

import java.util.UUID;

public record UploadClassification(
        UUID folderId,
        FileClassificationStatus status,
        FileClassificationSource source
) {
}
