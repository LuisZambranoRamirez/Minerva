package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;

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

    //revisa si es valodio el id del cliente el customerIdimpl
    public static CustomerIdImpl fromString(String value) throws InvalidDomainArgumentException {

        if (value == null || value.isBlank()) {
            throw new InvalidDomainArgumentException(
                    "El ID del cliente no puede estar vacío.");
        }

        try {
            return new CustomerIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidDomainArgumentException(
                    "El ID del cliente no tiene un formato UUID válido.",
                    e);
        }
    }
}
