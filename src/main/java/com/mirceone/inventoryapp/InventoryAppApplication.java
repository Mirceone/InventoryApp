package com.mirceone.inventoryapp;

import com.mirceone.inventoryapp.bootstrap.DBCreator;
import com.mirceone.inventoryapp.bootstrap.DotenvLoader;
import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppIntegrationProperties.class)
public class InventoryAppApplication {
    public static void main(String[] args) {
        DotenvLoader.loadIntoSystemPropertiesIfAbsent();
        // Creeaza DB-ul inainte de pornirea Spring/Flyway.
        DBCreator.createDatabaseIfMissing();
        SpringApplication.run(InventoryAppApplication.class, args);
    }
}
