package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;

public final class BooleanAttribute extends Attribute<Boolean> {
    private BooleanAttribute(String name, boolean attributeValue) throws NullValueException {
        super(name, attributeValue);
    }
}
