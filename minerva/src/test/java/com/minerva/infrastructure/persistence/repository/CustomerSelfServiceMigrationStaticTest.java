package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CustomerSelfServiceMigrationStaticTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    @Test
    void v7IsAdditiveAndKeepsClientePermissionSeedingOutOfSameEnumTransaction() throws IOException {
        String v7 = Files.readString(MIGRATIONS.resolve("V7__customer_self_service.sql"));

        assertTrue(v7.contains("ALTER TYPE role ADD VALUE IF NOT EXISTS 'CLIENTE'"));
        assertTrue(v7.contains("ADD COLUMN IF NOT EXISTS customer_id UUID"));
        assertTrue(v7.contains("ADD COLUMN IF NOT EXISTS approval_status account_approval_status NOT NULL DEFAULT 'APPROVED'"));
        assertTrue(v7.contains("ADD COLUMN IF NOT EXISTS business_name"));
        assertTrue(v7.contains("ADD COLUMN IF NOT EXISTS default_delivery_address"));
        assertTrue(v7.contains("ADD COLUMN IF NOT EXISTS delivery_address"));
        assertTrue(v7.contains("ADD COLUMN IF NOT EXISTS cancellation_reason"));
        assertTrue(v7.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_ruc_not_null"));
        assertTrue(v7.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_customer_id_not_null"));
        assertFalse(v7.contains("INSERT INTO role_permission"),
                "PostgreSQL enum values added in V7 must not be used for role_permission rows until a later migration");
    }

    @Test
    void v8SeedsClientePermissionsAndAbuseAttemptStorageAfterEnumValuesAreCommitted() throws IOException {
        String v8 = Files.readString(MIGRATIONS.resolve("V8__customer_self_service_permissions_and_abuse.sql"));

        assertTrue(v8.contains("INSERT INTO role_permission"));
        assertTrue(v8.contains("'CLIENTE', 'CUSTOMER_CATALOG_READ'"));
        assertTrue(v8.contains("'CLIENTE', 'CUSTOMER_ORDER_CREATE'"));
        assertTrue(v8.contains("'CLIENTE', 'CUSTOMER_ORDER_FIND_BY_ID'"));
        assertTrue(v8.contains("'CLIENTE', 'CUSTOMER_ORDER_FIND_ALL'"));
        assertTrue(v8.contains("'CLIENTE', 'CUSTOMER_ORDER_FIND_TRANSITIONS'"));
        assertTrue(v8.contains("'CLIENTE', 'CUSTOMER_ORDER_CANCEL'"));
        assertTrue(v8.contains("ON CONFLICT (id_role, id_permission) DO NOTHING"));
        assertTrue(v8.contains("CREATE TABLE IF NOT EXISTS abuse_attempt"));
    }
}