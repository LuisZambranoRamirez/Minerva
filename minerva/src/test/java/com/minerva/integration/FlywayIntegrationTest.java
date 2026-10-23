package com.minerva.integration;

import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayIntegrationTest extends IntegrationTest {

    private static final Set<String> EXPECTED_APPLICATION_TABLES = Set.of(
            "abuse_attempt",
            "app_user",
            "audit_event",
            "customer",
            "customer_order",
            "credit_note",
            "exception_log",
            "inventory_movement",
            "order_detail",
            "order_transition",
            "pay",
            "personal",
            "product",
            "product_modifier",
            "product_return",
            "role_permission",
            "sale",
            "sale_detail",
            "sale_detail_modifier",
            "stock_entry",
            "stock_loss",
            "stock_receipt",
            "supplier",
            "unit_to_bulk",
            "user_permission"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PostgreSQLContainer postgres;

    @Test
    void contextUsesPostgresql17ServiceConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(17, connection.getMetaData().getDatabaseMajorVersion());
        }
        assertTrue(postgres.getDockerImageName().startsWith("postgres:17"));
    }

    @Test
    void flywayRecordsVersionsOneThroughTenExactlyOnce() {
        List<String> appliedVersions = jdbcTemplate.queryForList("""
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank
                """, String.class);

        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), appliedVersions);
        assertEquals(10, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE NOT success", Integer.class));
    }

    @Test
    void migratedSchemaContainsCreditNotesAndAllApplicationTables() {
        Set<String> tables = Set.copyOf(jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class));

        List<String> roleValues = jdbcTemplate.queryForList("""
                SELECT enum_value.enumlabel
                FROM pg_enum enum_value
                JOIN pg_type enum_type ON enum_type.oid = enum_value.enumtypid
                WHERE enum_type.typname = 'role'
                ORDER BY enum_value.enumsortorder
                """, String.class);

        assertEquals(EXPECTED_APPLICATION_TABLES, tables);
        assertEquals(List.of("ADMIN", "VENDEDOR", "ALMACENISTA", "CLIENTE"), roleValues);
        assertEquals(6, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission WHERE id_role = CAST('CLIENTE' AS role)", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'product_return' AND column_name = 'credit_note_id' AND is_nullable <> 'NO'
                """, Integer.class));
    }
}
