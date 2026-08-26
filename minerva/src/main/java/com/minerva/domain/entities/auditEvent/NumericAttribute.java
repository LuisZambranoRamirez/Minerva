package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.ValueObject;

import java.math.BigDecimal;

public final class NumericAttribute extends Attribute<BigDecimal> {
    public NumericAttribute(String name, BigDecimal attributeValue) throws NullValueException {
        super(name, attributeValue);
    }

    public NumericAttribute(ValueObject<BigDecimal> valueObject) {
        super(valueObject.getClassName(), valueObject.getValue());
    }
}