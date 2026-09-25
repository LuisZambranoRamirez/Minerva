package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.inventory.InventoryMovementId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Set;
import java.util.UUID;

public final class InventoryMovementIdImpl extends ValueObject<UUID> implements InventoryMovementId {

    public InventoryMovementIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static InventoryMovementIdImpl generate() {
        try {
            return new InventoryMovementIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de movimiento de inventario: " + e.getMessage(),
                    e
            );
        }
    }

    public static InventoryMovementIdImpl fromString(String value) throws InvalidDomainArgumentException {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainArgumentException("El ID de movimiento de inventario no puede estar vacío.");
        }

        try {
            return new InventoryMovementIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidDomainArgumentException("El ID de movimiento de inventario no tiene un formato UUID válido.", e);
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
        return "inventoryMovementId";
    }

    @Override
    public String getAuditSubjectName() {
        return "inventoryMovementId";
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