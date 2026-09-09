package com.minerva.application.port.driven;

import com.minerva.domain.entities.auditEvent.AuditEvent;

public interface AuditService {
    void register(AuditEvent auditEvent);
}