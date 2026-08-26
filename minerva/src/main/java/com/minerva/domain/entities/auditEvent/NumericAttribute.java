package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;

import java.math.BigDecimal;

public final class NumericAttribute extends Attribute<BigDecimal> {
    public NumericAttribute(String name, BigDecimal attributeValue) throws NullValueException {
        super(name, attributeValue);
    }
}