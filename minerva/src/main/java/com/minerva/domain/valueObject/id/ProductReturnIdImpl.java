package com.minerva.domain.valueObject.id;

import java.util.Set;
import java.util.UUID;

import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.sale.ProductReturnId;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

public final class ProductReturnIdImpl extends ValueObject<UUID> implements ProductReturnId {

    public ProductReturnIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static ProductReturnIdImpl generate() {
        try {
            return new ProductReturnIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de devolución de producto: " + e.getMessage(),
                    e
            );
        }
    }

    public static ProductReturnIdImpl fromString(String value) throws NullValueException {
        if (value == null || value.isBlank()) throw new NullValueException("El ID de la devolución no puede estar vacío.");
        try {
            return new ProductReturnIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            throw new NullValueException("El ID de la devolución no tiene un formato UUID válido.");
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
        return "productReturnId";
    }

    @Override
    public String getAuditSubjectName() {
        return "productReturnId";
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
