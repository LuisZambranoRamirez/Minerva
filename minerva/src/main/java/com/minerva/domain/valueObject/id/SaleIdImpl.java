package com.minerva.domain.valueObject.id;

import java.util.Set;
import java.util.UUID;

import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.sale.SaleId;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;

public final class SaleIdImpl extends ValueObject<UUID> implements SaleId {

    public SaleIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static SaleIdImpl generate() {
        try {
            return new SaleIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de venta: " + e.getMessage(),
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
        return "saleId";
    }

    @Override
    public String getAuditSubjectName() {
        return "saleId";
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
