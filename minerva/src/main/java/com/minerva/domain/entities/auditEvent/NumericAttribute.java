package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.valueObject.ValueObject;

import java.math.BigDecimal;

public final class NumericAttribute extends Attribute<BigDecimal> {
    public NumericAttribute(String name, BigDecimal attributeValue) {
        super(name, attributeValue);
    }

    public NumericAttribute(String name, ValueObject<BigDecimal> attributeValue) {
        super(name, attributeValue.getValue());
    }

    public NumericAttribute(ValueObject<BigDecimal> valueObject) {
        super(valueObject.getClassName(), valueObject.getValue());
    }
}