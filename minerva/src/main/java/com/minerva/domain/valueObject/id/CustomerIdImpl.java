package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Set;
import java.util.UUID;

public final class CustomerIdImpl extends ValueObject<UUID> implements CustomerId {

    public CustomerIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static CustomerIdImpl generate() {
        try {
            return new CustomerIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de customer: " + e.getMessage(),
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
        return "customerId";
    }

    @Override
    public String getAuditSubjectName() {
        return "customerId";
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
