package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;

import java.util.Objects;
import java.util.Optional;

public sealed  abstract class Attribute<V> permits BooleanAttribute, DateTimeAttribute, NumericAttribute, StringAttribute {
    private final String name;
    private final V attributeValue;

    protected Attribute(String name, V attributeValue) throws NullValueException {
        if (name == null) throw new NullValueException("El nombre del atributo no puede ser nulo.");
        this.name = name;
        this.attributeValue = attributeValue;
    }

    public String getName() {
        return name;
    }

    public Optional<V> getAttributeValue() {
        return Optional.ofNullable(attributeValue);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Attribute<?> attribute = (Attribute<?>) o;
        return Objects.equals(getName(), attribute.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }
}