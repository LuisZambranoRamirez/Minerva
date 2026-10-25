package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryReportMigrationStaticTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    @Test
    void v12AddsPermissionOutsideATransactionAndGrantsOnlyStaffRolesIdempotently() throws IOException {
        String v12 = Files.readString(MIGRATIONS.resolve(
                "V12__grant_inventory_report_export_permission.sql"));
        String configuration = Files.readString(MIGRATIONS.resolve(
                "V12__grant_inventory_report_export_permission.sql.conf"));

        assertTrue(configuration.contains("executeInTransaction=false"));
        assertTrue(v12.contains(
                "ALTER TYPE permission ADD VALUE IF NOT EXISTS 'PRODUCT_EXPORT_INVENTORY_REPORT'"));
        assertTrue(v12.contains("('ADMIN', 'PRODUCT_EXPORT_INVENTORY_REPORT')"));
        assertTrue(v12.contains("('VENDEDOR', 'PRODUCT_EXPORT_INVENTORY_REPORT')"));
        assertTrue(v12.contains("('ALMACENISTA', 'PRODUCT_EXPORT_INVENTORY_REPORT')"));
        assertFalse(v12.contains("('CLIENTE', 'PRODUCT_EXPORT_INVENTORY_REPORT')"));
        assertTrue(v12.contains("ON CONFLICT (id_role, id_permission) DO NOTHING"));
    }
}
