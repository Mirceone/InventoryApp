package com.mirceone.inventoryapp.integration;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * La fiecare pornire a contextului Spring cu profilul {@code test}, schema este
 * curatata cu Flyway {@code clean()} si reaplicate migrarile — echivalent cu o baza
 * proaspat creata cu aceleasi scripturi ca in productie.
 */
@Configuration
@Profile("test")
public class IntegrationTestFlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
