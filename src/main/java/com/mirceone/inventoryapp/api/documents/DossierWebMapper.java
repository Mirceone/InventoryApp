package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.service.documents.DossierSummary;

public final class DossierWebMapper {

    private DossierWebMapper() {
    }

    public static DossierResponse toResponse(DossierSummary s) {
        return new DossierResponse(
                s.id(),
                s.firmId(),
                s.name(),
                s.createdByUserId(),
                s.createdAt(),
                s.documentCount()
        );
    }
}
