package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Auditable;

public interface Id<I> extends Auditable {
    I getIdValue();
    String getIdValueAsString();
    String getIdName();
}
