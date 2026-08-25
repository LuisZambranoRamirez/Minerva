package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.userAction.Auditable;

public interface Id<I> extends Auditable {
    I getIdValue();
    String asString();
}
