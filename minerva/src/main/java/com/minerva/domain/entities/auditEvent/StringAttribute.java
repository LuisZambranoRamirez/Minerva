package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.valueObject.id.Id;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class StringAttribute extends Attribute<String> {
    public StringAttribute(String name, String attributeValue) {
        super(name, attributeValue);
    }

    public StringAttribute(Enum<?> a) {
        super(a.getClass().getSimpleName(), a.name());
    }

    public StringAttribute(ValueObject<String> valueObject) {
        super(valueObject.getClassName(), valueObject.getValue());
    }

    public StringAttribute(Id<?> id) {
        super(id.getIdName(), id.getIdValueAsString());
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StringAttribute(String name, LocalDateTime attributeValue) {
        super(
                name,
                attributeValue == null
                        ? null
                        : attributeValue.format(DATE_TIME_FORMATTER)
        );
    }
}