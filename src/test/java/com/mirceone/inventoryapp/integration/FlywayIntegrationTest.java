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
    void flywayCreatesCoreTables() {
        String schema = currentSchema();
        // table_schema + table_type evita randuri duplicate in H2 (ex. alias-uri in information_schema)
        String base = """
                select count(*) from information_schema.tables
                where lower(table_name) = ?
                  and table_type = 'BASE TABLE'
                  and lower(table_schema) = ?
                """;
        Integer usersTable = jdbcTemplate.queryForObject(base, Integer.class, "users", schema);
        Integer refreshTokensTable = jdbcTemplate.queryForObject(base, Integer.class, "refresh_tokens", schema);
        Integer stockEventsTable = jdbcTemplate.queryForObject(base, Integer.class, "stock_change_events", schema);
        Integer passwordResetTokensTable = jdbcTemplate.queryForObject(base, Integer.class, "password_reset_tokens", schema);
        Integer firmWorkOrdersTable = jdbcTemplate.queryForObject(base, Integer.class, "firm_work_orders", schema);
        Integer workOrderFoldersTable = jdbcTemplate.queryForObject(base, Integer.class, "work_order_folders", schema);
        Integer workOrderFolderRulesTable = jdbcTemplate.queryForObject(base, Integer.class, "work_order_folder_rules", schema);
        Integer workOrderFilesTable = jdbcTemplate.queryForObject(base, Integer.class, "work_order_files", schema);
        Integer workOrderInvoicesTable = jdbcTemplate.queryForObject(base, Integer.class, "work_order_invoices", schema);
        Integer legacyFirmDocumentsTable = jdbcTemplate.queryForObject(base, Integer.class, "firm_documents", schema);
        Integer firmInvitationsTable = jdbcTemplate.queryForObject(base, Integer.class, "firm_invitations", schema);
        Integer firmStatusHistoryTable = jdbcTemplate.queryForObject(base, Integer.class, "firm_status_history", schema);
        Integer notificationsTable = jdbcTemplate.queryForObject(base, Integer.class, "notifications", schema);
        Integer ownershipTransferConfirmationsTable =
                jdbcTemplate.queryForObject(base, Integer.class, "firm_ownership_transfer_confirmations", schema);

        assertEquals(1, usersTable);
        assertEquals(1, refreshTokensTable);
        assertEquals(1, stockEventsTable);
        assertEquals(1, passwordResetTokensTable);
        assertEquals(1, firmWorkOrdersTable);
        assertEquals(1, workOrderFoldersTable);
        assertEquals(1, workOrderFolderRulesTable);
        assertEquals(1, workOrderFilesTable);
        assertEquals(1, workOrderInvoicesTable);
        assertEquals(0, legacyFirmDocumentsTable);
        assertEquals(1, firmInvitationsTable);
        assertEquals(1, firmStatusHistoryTable);
        assertEquals(1, notificationsTable);
        assertEquals(1, ownershipTransferConfirmationsTable);
    }

    @Test
    void flywaySchemaHistoryIsAppliedUpToV8() {
        Integer maxVersion = jdbcTemplate.queryForObject(
                "select max(cast(version as integer)) from flyway_schema_history where success = true",
                Integer.class
        );
        assertTrue(maxVersion != null && maxVersion >= 8);
    }

    @Test
    void firmWorkOrdersHasProjectMetadataColumns() {
        String schema = currentSchema();
        assertEquals(1, columnExists(schema, "firm_work_orders", "client_name"));
        assertEquals(1, columnExists(schema, "firm_work_orders", "location"));
        assertEquals(1, columnExists(schema, "firm_work_orders", "description"));
        assertEquals(1, columnExists(schema, "firm_work_orders", "estimated_end_date"));
        assertEquals(1, columnExists(schema, "firm_work_orders", "status"));
    }

    @Test
    void workOrderFoldersHasTreeColumns() {
        String schema = currentSchema();
        assertEquals(1, columnExists(schema, "work_order_folders", "parent_id"));
        assertEquals(1, columnExists(schema, "work_order_folders", "catch_all"));
        assertEquals(1, columnExists(schema, "work_order_folders", "sort_order"));
    }

    @Test
    void workOrderInvoicesHasProcessingColumns() {
        String schema = currentSchema();
        assertEquals(1, columnExists(schema, "work_order_invoices", "processing_status"));
        assertEquals(1, columnExists(schema, "work_order_invoices", "markdown_text"));
        assertEquals(1, columnExists(schema, "work_order_invoices", "processing_error"));
        assertEquals(1, columnExists(schema, "work_order_invoices", "storage_key"));
    }

    @Test
    void workOrderFilesHasOpaqueStorageColumns() {
        String schema = currentSchema();
        assertEquals(1, columnExists(schema, "work_order_files", "folder_id"));
        assertEquals(1, columnExists(schema, "work_order_files", "display_name"));
        assertEquals(1, columnExists(schema, "work_order_files", "extension"));
        assertEquals(1, columnExists(schema, "work_order_files", "storage_key"));
        assertEquals(0, columnExists(schema, "work_order_files", "processing_status"));
        assertEquals(0, columnExists(schema, "work_order_files", "folder_path"));
    }

    @Test
    void workOrderInvoicePendingIndexIsPresent() {
        String schema = currentSchema();
        String pendingIndex = jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = ?
                  and indexname = 'idx_work_order_invoices_pending'
                """,
                String.class,
                schema
        );
        assertTrue(pendingIndex != null && pendingIndex.toLowerCase().contains("where"));
        assertTrue(pendingIndex.toLowerCase().contains("pending"));
    }

    @Test
    void workOrderIndexesArePresent() {
        String schema = currentSchema();
        String workOrderIndex = jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = ?
                  and indexname = 'idx_firm_work_orders_firm_created'
                """,
                String.class,
                schema
        );
        String catchAllIndex = jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = ?
                  and indexname = 'uk_work_order_folders_catch_all'
                """,
                String.class,
                schema
        );
        String fileNameIndex = jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = ?
                  and indexname = 'uk_work_order_files_folder_display_name'
                """,
                String.class,
                schema
        );

        assertTrue(workOrderIndex != null && workOrderIndex.contains("firm_id"));
        assertTrue(catchAllIndex != null && catchAllIndex.toLowerCase().contains("where"));
        assertTrue(fileNameIndex != null && fileNameIndex.toLowerCase().contains("lower"));
    }

    @Test
    void firmInvitationAndOwnershipTransferIndexesArePresent() {
        String schema = currentSchema();
        String invitationIndex = jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = ?
                  and indexname = 'uk_firm_invitations_pending_email'
                """,
                String.class,
                schema
        );
        String ownershipLookupIndex = jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = ?
                  and indexname = 'idx_firm_ownership_transfer_confirmations_lookup'
                """,
                String.class,
                schema
        );

        assertTrue(invitationIndex != null && invitationIndex.toLowerCase().contains("where"));
        assertTrue(invitationIndex != null && invitationIndex.toLowerCase().contains("status"));
        assertTrue(ownershipLookupIndex != null && ownershipLookupIndex.contains("created_at DESC"));
    }

    private String currentSchema() {
        return jdbcTemplate.queryForObject("select current_schema()", String.class);
    }

    private Integer columnExists(String schema, String table, String column) {
        return jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.columns
                where lower(table_name) = ?
                  and lower(column_name) = ?
                  and lower(table_schema) = ?
                """,
                Integer.class,
                table,
                column,
                schema
        );
    }
}
