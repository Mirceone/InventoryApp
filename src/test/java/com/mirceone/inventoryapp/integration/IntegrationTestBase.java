package com.mirceone.inventoryapp.integration;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Baza pentru teste de integrare pe PostgreSQL real (fara Testcontainers).
 * <p>
 * Config: {@code src/test/resources/application-test.yml} — baza dedicata testelor
 * (ex. {@code inventoryapp_test}), aceleasi migrari Flyway ca in productie.
 * La startup (profil {@code test}), Flyway ruleaza {@code clean} apoi {@code migrate}.
 * </p>
 */
@ActiveProfiles("test")
@Import(IntegrationTestFlywayConfig.class)
public abstract class IntegrationTestBase {
}
