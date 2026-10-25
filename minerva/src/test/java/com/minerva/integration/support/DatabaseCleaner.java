package com.minerva.integration.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DatabaseCleaner {

    private static final String ANONYMOUS_NAME = "anonimo";

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "abuse_attempt",
            "app_user",
            "audit_event",
            "customer",
            "customer_order",
            "credit_note",
            "demo_catalog_seed",
            "exception_log",
            "flyway_schema_history",
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

    private static final Set<String> PRESERVED_TABLES = Set.of(
            "flyway_schema_history",
            "role_permission",
            "customer",
            "supplier"
    );

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        List<String> discoveredTables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """, String.class);

        Set<String> actualTables = new HashSet<>(discoveredTables);
        if (!actualTables.equals(EXPECTED_TABLES)) {
            Set<String> missing = new HashSet<>(EXPECTED_TABLES);
            missing.removeAll(actualTables);
            Set<String> unknown = new HashSet<>(actualTables);
            unknown.removeAll(EXPECTED_TABLES);
            throw new IllegalStateException(
                    "Database cleaner classification is stale. Missing=" + missing + ", unknown=" + unknown);
        }

        String mutableTables = discoveredTables.stream()
                .filter(table -> !PRESERVED_TABLES.contains(table))
                .map(table -> '"' + table + '"')
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();

        jdbcTemplate.execute("TRUNCATE TABLE " + mutableTables + " RESTART IDENTITY CASCADE");
        jdbcTemplate.update("DELETE FROM customer WHERE full_name <> ?", ANONYMOUS_NAME);
        jdbcTemplate.update("DELETE FROM supplier WHERE supplier_name <> ?", ANONYMOUS_NAME);

        assertSingleAnonymousRow("customer", "full_name");
        assertSingleAnonymousRow("supplier", "supplier_name");
    }

    private void assertSingleAnonymousRow(String table, String nameColumn) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + nameColumn + " = ?",
                Integer.class,
                ANONYMOUS_NAME
        );
        if (count == null || count != 1) {
            throw new IllegalStateException(
                    "Expected exactly one preserved anonymous row in " + table + " but found " + count);
        }
    }
}
