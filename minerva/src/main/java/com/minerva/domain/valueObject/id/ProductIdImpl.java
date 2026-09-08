package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Set;
import java.util.UUID;

public final class ProductIdImpl extends ValueObject<UUID> implements ProductId {

    public ProductIdImpl(UUID value) throws NullValueException {
        if (value == null) throw new NullValueException("El productId no puede ser nulo");
        super(value);
    }

    public static ProductIdImpl generate() {
        try {
            return new ProductIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de product: " + e.getMessage(),
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
        return "productId";
    }

    @Override
    public String getAuditSubjectName() {
        return "productId";
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
