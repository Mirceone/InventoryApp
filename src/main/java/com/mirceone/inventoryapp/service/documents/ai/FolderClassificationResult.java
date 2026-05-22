package com.mirceone.inventoryapp.service.documents.ai;

import com.mirceone.inventoryapp.model.OrganizationSource;

public record FolderClassificationResult(String folderPath, OrganizationSource source) {
}
