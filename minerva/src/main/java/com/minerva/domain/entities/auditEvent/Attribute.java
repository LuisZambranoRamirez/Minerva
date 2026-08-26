package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.id.Id;

import java.util.Objects;
import java.util.Optional;

public sealed abstract class Attribute<A> permits BooleanAttribute, NumericAttribute, StringAttribute {
    private final String name;
    private final A attributeValue;

    protected Attribute(String name, A attributeValue) {
        if (name == null) throw new UnexpectedDomainException("El nombre del atributo no puede ser nulo.");
        this.name = name;
        this.attributeValue = attributeValue;
    }

    public String getName() {
        return name;
    }

    public Optional<A> getAttributeValue() {
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