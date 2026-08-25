package com.minerva.domain.valueObject.id;

import java.util.Map;
import java.util.UUID;

import com.minerva.domain.entities.stockEntry.StockEntryId;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.exceptions.UnexpectedDomainException;

public class StockEntryIdImpl extends ValueObject<UUID> implements StockEntryId {
    public StockEntryIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static StockEntryIdImpl generate() {
        try {
            return new StockEntryIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException("Error al generar el ID de entrada de stock: " + e.getMessage(), e);
        }
    }

    @Override
    public UUID getIdValue() {
        return getValue();
    }

    @Override
    public String asString() {
        return getValue().toString();
    }

    @Override
    public Map<String, Attribute<?>> extractAuditData() {
        return Map.of("StockEntryId", new DefaultStringAttribute(asString()));
    }
}
