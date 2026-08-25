package com.minerva.domain.valueObject.id;

import java.util.Map;
import java.util.UUID;

import com.minerva.domain.entities.sale.SaleDetailId;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.valueObject.ValueObject;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;

public class SaleDetailIdImpl extends ValueObject<UUID> implements SaleDetailId {
    
    public SaleDetailIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static SaleDetailIdImpl generate() {
        try {
            return new SaleDetailIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException("Error al generar el ID de detalle de venta: " + e.getMessage(), e);
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
        return Map.of("SaleDetailId", new DefaultStringAttribute(asString()));
    }
}
