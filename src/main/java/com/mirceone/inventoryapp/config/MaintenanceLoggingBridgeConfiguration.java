package com.mirceone.inventoryapp.config;

import com.mirceone.inventoryapp.ops.MaintenanceLogRing;
import com.mirceone.inventoryapp.ops.MaintenanceRingAppender;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaintenanceLoggingBridgeConfiguration {

    private final MaintenanceLogRing ring;

    public MaintenanceLoggingBridgeConfiguration(MaintenanceLogRing ring) {
        this.ring = ring;
    }

    @PostConstruct
    void wireRingIntoAppender() {
        MaintenanceRingAppender.setRing(ring);
    }
}
