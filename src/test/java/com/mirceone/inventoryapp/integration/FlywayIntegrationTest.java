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
        Integer usersTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'users'",
                Integer.class
        );
        Integer refreshTokensTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'refresh_tokens'",
                Integer.class
        );
        Integer stockEventsTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'stock_change_events'",
                Integer.class
        );

        assertEquals(1, usersTable);
        assertEquals(1, refreshTokensTable);
        assertEquals(1, stockEventsTable);
    }

    @Test
    void flywaySchemaHistoryIsAppliedUpToV6() {
        Integer maxVersion = jdbcTemplate.queryForObject(
                "select max(cast(version as integer)) from flyway_schema_history where success = true",
                Integer.class
        );
        assertTrue(maxVersion != null && maxVersion >= 6);
    }
}
