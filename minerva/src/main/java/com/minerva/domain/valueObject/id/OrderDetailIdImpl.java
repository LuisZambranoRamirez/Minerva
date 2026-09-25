package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.order.OrderDetailId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Set;
import java.util.UUID;

public final class OrderDetailIdImpl extends ValueObject<UUID> implements OrderDetailId {

    public OrderDetailIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static OrderDetailIdImpl generate() {
        try {
            return new OrderDetailIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de detalle de pedido: " + e.getMessage(),
                    e
            );
        }
    }

    public static OrderDetailIdImpl fromString(String value) throws InvalidDomainArgumentException {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainArgumentException("El ID de detalle de pedido no puede estar vacío.");
        }

        try {
            return new OrderDetailIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidDomainArgumentException("El ID de detalle de pedido no tiene un formato UUID válido.", e);
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
        return "orderDetailId";
    }

    @Override
    public String getAuditSubjectName() {
        return "orderDetailId";
    }

    @Override
    public Id<?> getAuditSubjectId() {
        return this;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(new StringAttribute(getAuditSubjectId()));
    }
}