package com.mirceone.inventoryapp.integration;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Baza pentru teste de integrare: H2 in-memory + migrari Flyway in {@code db/migration/h2}
 * (echivalent logic cu PostgreSQL din productie; fara {@code pgcrypto} / {@code gen_random_uuid}).
 * <p>
 * La startup (profil {@code test}), Flyway ruleaza {@code clean} apoi {@code migrate}.
 * </p>
 */
@ActiveProfiles("test")
@Import(IntegrationTestFlywayConfig.class)
public abstract class IntegrationTestBase {
}
