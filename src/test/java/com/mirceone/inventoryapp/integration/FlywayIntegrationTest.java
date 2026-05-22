package com.mirceone.inventoryapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FlywayIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreTablesIncludingAuditAndRefreshTokens() {
        // table_schema + table_type evita randuri duplicate in H2 (ex. alias-uri in information_schema)
        String base = """
                select count(*) from information_schema.tables
                where lower(table_name) = ?
                  and table_type = 'BASE TABLE'
                  and upper(table_schema) = 'PUBLIC'
                """;
        Integer usersTable = jdbcTemplate.queryForObject(base, Integer.class, "users");
        Integer refreshTokensTable = jdbcTemplate.queryForObject(base, Integer.class, "refresh_tokens");
        Integer stockEventsTable = jdbcTemplate.queryForObject(base, Integer.class, "stock_change_events");
        Integer passwordResetTokensTable = jdbcTemplate.queryForObject(base, Integer.class, "password_reset_tokens");
        Integer firmDocumentsTable = jdbcTemplate.queryForObject(base, Integer.class, "firm_documents");
        Integer firmDossiersTable = jdbcTemplate.queryForObject(base, Integer.class, "firm_dossiers");

        assertEquals(1, usersTable);
        assertEquals(1, refreshTokensTable);
        assertEquals(1, stockEventsTable);
        assertEquals(1, passwordResetTokensTable);
        assertEquals(1, firmDocumentsTable);
        assertEquals(1, firmDossiersTable);
    }

    @Test
    void flywaySchemaHistoryIsAppliedUpToV16() {
        Integer maxVersion = jdbcTemplate.queryForObject(
                "select max(cast(version as integer)) from flyway_schema_history where success = true",
                Integer.class
        );
        assertTrue(maxVersion != null && maxVersion >= 16);
    }

    @Test
    void firmDocumentsHasDossierId() {
        Integer col = jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.columns
                where lower(table_name) = 'firm_documents'
                  and lower(column_name) = 'dossier_id'
                  and upper(table_schema) = 'PUBLIC'
                """,
                Integer.class
        );
        assertEquals(1, col);
    }

    @Test
    void firmDocumentsHasOrganizationColumns() {
        Integer col = jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.columns
                where lower(table_name) = 'firm_documents'
                  and lower(column_name) = 'processing_status'
                  and upper(table_schema) = 'PUBLIC'
                """,
                Integer.class
        );
        assertEquals(1, col);
    }
}
