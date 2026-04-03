package com.mirceone.inventoryapp;

import com.mirceone.inventoryapp.bootstrap.DBCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryAppApplication {
    public static void main(String[] args) {
        // Creeaza DB-ul inainte de pornirea Spring/Flyway.
        DBCreator.createDatabaseIfMissing();
        SpringApplication.run(InventoryAppApplication.class, args);
    }
}
