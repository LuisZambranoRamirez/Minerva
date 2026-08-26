package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;

public final class StringAttribute extends Attribute<String> {
    public StringAttribute(String name, String attributeValue) throws NullValueException {
        super(name, attributeValue);
    }
}