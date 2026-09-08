package com.minerva.domain.valueObject.id;

import java.util.Set;
import java.util.UUID;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.AuditEventId;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

public final class AuditEventIdImpl extends ValueObject<UUID> implements AuditEventId {

    private AuditEventIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static AuditEventIdImpl generate() {
        try {
            return new AuditEventIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de acción de usuario: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public UUID getIdValue() {
        return getValue();
    }

    @Override
    public String getIdValueAsString() {
        return getValue().toString();
    }

    @Override
    public String getIdName() {
        return "auditEventId";
    }

    @Override
    public String getAuditSubjectName() {
        return "auditEventId";
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