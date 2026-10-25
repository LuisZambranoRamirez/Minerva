package com.minerva.domain.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryReportSecurityTest {

    @Test
    void inventoryReportExportPermissionExistsAndIsAssignedOnlyToStaffRoles() {
        Permission permission = Permission.valueOf("PRODUCT_EXPORT_INVENTORY_REPORT");

        assertSame(Permission.PRODUCT_EXPORT_INVENTORY_REPORT, permission);
        assertTrue(Role.ADMIN.hasPermission(permission));
        assertTrue(Role.VENDEDOR.hasPermission(permission));
        assertTrue(Role.ALMACENISTA.hasPermission(permission));
        assertFalse(Role.CLIENTE.hasPermission(permission));
    }
}
