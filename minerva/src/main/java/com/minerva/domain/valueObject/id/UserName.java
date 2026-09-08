package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Set;

public final class UserName extends ValueObject<String> implements UserId {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;

    public UserName(String value) throws InvalidDomainArgumentException {
        super(value);
        if (value.isBlank()) throw new InvalidDomainArgumentException("El USERNAME no puede estar vacío.");
        if (value.length() < MIN_LENGTH) throw new InvalidDomainArgumentException("El USERNAME debe tener al menos " + MIN_LENGTH + " caracteres.");
        if (value.length() > MAX_LENGTH) throw new InvalidDomainArgumentException("El USERNAME no puede exceder los " + MAX_LENGTH + " caracteres.");
        if (!value.matches("^[a-zA-Z0-9]+$")) throw new InvalidDomainArgumentException("El USERNAME solo puede contener letras (sin tilde) y números");
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
        return "userId";
    }

    @Override
    public String getAuditSubjectName() {
        return "userId";
    }

    @Override
    public Id<?> getAuditSubjectId() {
        return this;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getAuditSubjectId())
        );
    }
}
