package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.valueObject.id.Id;

import java.util.Set;

public interface Auditable {
    String getAuditSubjectName();
    Id<?> getAuditSubjectId();
    Set<Attribute<?>> getAuditData();
}
