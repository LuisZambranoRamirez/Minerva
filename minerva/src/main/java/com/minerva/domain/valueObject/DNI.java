package com.minerva.domain.valueObject;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.id.Id;

import java.util.Set;

public class DNI extends ValueObject<String> implements Id<String> {

    private static final int LENGTH = 8;

    public DNI(String value) throws InvalidDomainArgumentException {
        super(value);

        if (value.isBlank()) throw new InvalidDomainArgumentException("El DNI no puede estar vacío.");
        if (!value.matches("^\\d+$")) throw new InvalidDomainArgumentException("El DNI solo puede contener números.");
        if (value.length() != LENGTH) throw new InvalidDomainArgumentException("El DNI debe tener exactamente " + LENGTH + " dígitos.");
    }

    @Override
    public String getIdValue() {
        return getValue();
    }

    @Override
    public String getIdValueAsString() {
        return getValue();
    }

    @Override
    public String getIdName() {
        return "dni";
    }

    @Override
    public String getAuditSubjectName() {
        return "Personal";
    }

    @Override
    public Id<?> getAuditSubjectId() {
        return this;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(new StringAttribute((Id<?>) this));
    }
}
