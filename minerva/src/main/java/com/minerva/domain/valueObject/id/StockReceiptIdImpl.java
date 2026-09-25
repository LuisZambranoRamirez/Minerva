package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.stockReceipt.StockReceiptId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Set;
import java.util.UUID;

public final class StockReceiptIdImpl extends ValueObject<UUID> implements StockReceiptId {

    public StockReceiptIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static StockReceiptIdImpl generate() {
        try {
            return new StockReceiptIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException("Error al generar el ID de recepción de stock.", e);
        }
    }

    public static StockReceiptIdImpl fromString(String value) throws InvalidDomainArgumentException {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainArgumentException("El ID de la recepción no puede estar vacío.");
        }

        try {
            return new StockReceiptIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidDomainArgumentException("El ID de la recepción no tiene un formato UUID válido.", e);
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
        return "stockReceiptId";
    }

    @Override
    public String getAuditSubjectName() {
        return "StockReceipt";
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
