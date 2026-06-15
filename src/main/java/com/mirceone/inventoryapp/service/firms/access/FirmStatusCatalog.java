package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.FirmStatus;

import java.util.Locale;

public final class FirmStatusCatalog {

    private FirmStatusCatalog() {
    }

    /**
     * UI label derived from API enum: ACTIVE → Active, PAUSED → Paused, CRITICAL → Critical.
     */
    public static String displayLabel(FirmStatus status) {
        if (status == null) {
            return "";
        }
        String name = status.name();
        if (name.isEmpty()) {
            return name;
        }
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
