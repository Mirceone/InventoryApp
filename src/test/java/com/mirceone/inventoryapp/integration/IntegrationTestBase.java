package com.mirceone.inventoryapp.integration;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

/**
 * Baza pentru teste de integrare pe PostgreSQL (baza dedicata, ex. {@code inventoryapp_test}).
 * Migrarile Flyway sunt cele din productie ({@code src/main/resources/db/migration}).
 * <p>
 * La startup (profil {@code test}), Flyway ruleaza {@code clean} apoi {@code migrate}
 * pe o schema izolata per run, pentru a evita conflictele cu alte rulari locale/CI care
 * folosesc aceeasi baza de test.
 * </p>
 */
@ActiveProfiles("test")
@Import(IntegrationTestFlywayConfig.class)
public abstract class IntegrationTestBase {

    private static final String TEST_SCHEMA = "it_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void registerIsolatedSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.schema", () -> TEST_SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> TEST_SCHEMA);
        registry.add("spring.flyway.default-schema", () -> TEST_SCHEMA);
        registry.add("spring.flyway.schemas", () -> TEST_SCHEMA);
        registry.add("spring.flyway.create-schemas", () -> true);
    }
}
