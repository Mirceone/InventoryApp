package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.FirmStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirmStatusCatalogTest {

    @Test
    void displayLabels() {
        assertEquals("Active", FirmStatusCatalog.displayLabel(FirmStatus.ACTIVE));
        assertEquals("Paused", FirmStatusCatalog.displayLabel(FirmStatus.PAUSED));
        assertEquals("Critical", FirmStatusCatalog.displayLabel(FirmStatus.CRITICAL));
    }
}
