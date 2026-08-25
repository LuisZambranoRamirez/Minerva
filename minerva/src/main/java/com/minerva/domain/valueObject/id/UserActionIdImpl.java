package com.minerva.domain.valueObject.id;

import java.util.Map;
import java.util.UUID;

import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.entities.userAction.UserActionId;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

public class UserActionIdImpl extends ValueObject<UUID> implements UserActionId {
    
    private UserActionIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static UserActionIdImpl generate() {
        try {
            return new UserActionIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException("Error al generar el ID de acción de usuario: " + e.getMessage(), e);
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
        return Map.of("UserActionId", new DefaultStringAttribute(asString()));
    }
}
