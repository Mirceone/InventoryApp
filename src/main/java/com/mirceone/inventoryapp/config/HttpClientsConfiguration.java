package com.mirceone.inventoryapp.config;

import com.mirceone.inventoryapp.ops.MaintenanceLogRing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientsConfiguration {

    @Bean
    MaintenanceLogRing maintenanceLogRing(AppIntegrationProperties props) {
        return new MaintenanceLogRing(props.getOps().getLogRingMaxLines(), props.getOps().isLogRingEnabled());
    }
}
