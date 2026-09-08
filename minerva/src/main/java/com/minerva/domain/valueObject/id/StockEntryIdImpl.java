package com.minerva.domain.valueObject.id;

import java.util.Set;
import java.util.UUID;

import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.stockEntry.StockEntryId;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.exceptions.UnexpectedDomainException;

public final class StockEntryIdImpl extends ValueObject<UUID> implements StockEntryId {

    public StockEntryIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static StockEntryIdImpl generate() {
        try {
            return new StockEntryIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de entrada de stock: " + e.getMessage(),
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
        return "stockEntryId";
    }

    @Override
    public String getAuditSubjectName() {
        return "stockEntryId";
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
