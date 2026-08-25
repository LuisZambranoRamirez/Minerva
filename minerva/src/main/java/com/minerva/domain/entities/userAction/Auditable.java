package com.minerva.domain.entities.userAction;

import java.util.Map;

public interface Auditable {
    String getAuditSubject();
    Map<String, Attribute<?>> extractAuditData();
}
