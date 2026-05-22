package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.FirmStatus;

import java.util.Map;

public final class FirmStatusCatalog {

    private static final Map<FirmStatus, String> DISPLAY_LABELS = Map.of(
            FirmStatus.ACTIVE, "Activ",
            FirmStatus.PAUSED, "În pauză",
            FirmStatus.CRITICAL, "Critic"
    );

    private FirmStatusCatalog() {
    }

    public static String displayLabel(FirmStatus status) {
        return DISPLAY_LABELS.getOrDefault(status, status.name());
    }
}
