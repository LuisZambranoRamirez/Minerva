package com.minerva.domain.entities.auditEvent;

import java.util.List;

public interface Auditable {
    String getAuditSubject();
    List<Attribute<?>> extractAuditData();
}
