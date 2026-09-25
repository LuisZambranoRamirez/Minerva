package com.minerva.domain.valueObject.id;

import java.util.Set;
import java.util.UUID;

import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.sale.SaleDetailId;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;

public final class SaleDetailIdImpl extends ValueObject<UUID> implements SaleDetailId {

    public SaleDetailIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static SaleDetailIdImpl generate() {
        try {
            return new SaleDetailIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de detalle de venta: " + e.getMessage(),
                    e
            );
        }
    }

    public static SaleDetailIdImpl fromString(String value) throws NullValueException {
        if (value == null || value.isBlank()) throw new NullValueException("El ID del detalle de venta no puede estar vacío.");
        try {
            return new SaleDetailIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            throw new NullValueException("El ID del detalle de venta no tiene un formato UUID válido.");
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
        return "saleDetailId";
    }

    @Override
    public String getAuditSubjectName() {
        return "saleDetailId";
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
