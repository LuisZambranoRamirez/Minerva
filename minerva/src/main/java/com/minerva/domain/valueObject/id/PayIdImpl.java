package com.minerva.domain.valueObject.id;

import java.util.Map;
import java.util.UUID;

import com.minerva.domain.entities.sale.PayId;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.ValueObject;

public class PayIdImpl extends ValueObject<UUID> implements PayId {

    public PayIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static PayIdImpl generate() {
        try {
            return new PayIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException("Error al generar el ID de pago: " + e.getMessage(), e);
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
        return Map.of("PayId", new DefaultStringAttribute(asString()));
    }
}
