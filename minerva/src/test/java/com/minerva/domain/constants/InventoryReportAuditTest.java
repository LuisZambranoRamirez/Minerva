package com.minerva.domain.constants;

import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryReportAuditTest {

    @Test
    void exportPermissionIsClassifiedAsRead() {
        assertEquals(AuditEventType.READ,
                AuditEventType.fromPermission(Permission.PRODUCT_EXPORT_INVENTORY_REPORT));
    }

    @Test
    void inventoryReportCollectionHasStableAuditIdentity() {
        CollectionAuditTarget target = new CollectionAuditTarget(
                CollectionAuditTarget.Resource.INVENTORY_REPORTS);

        assertEquals("ALL", target.getIdValue());
        assertEquals("INVENTORY_REPORTS", target.getAuditSubjectName());
        assertEquals("ALL", target.getAuditSubjectId().getIdValueAsString());
    }
}
