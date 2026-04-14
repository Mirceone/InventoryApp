package com.mirceone.inventoryapp.integration;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Baza pentru teste de integrare pe PostgreSQL (baza dedicata, ex. {@code inventoryapp_test}).
 * Migrarile Flyway sunt cele din productie ({@code src/main/resources/db/migration}).
 * <p>
 * La startup (profil {@code test}), Flyway ruleaza {@code clean} apoi {@code migrate}.
 * Nu folosi baza de productie pentru teste.
 * </p>
 */
@ActiveProfiles("test")
@Import(IntegrationTestFlywayConfig.class)
public abstract class IntegrationTestBase {
}
