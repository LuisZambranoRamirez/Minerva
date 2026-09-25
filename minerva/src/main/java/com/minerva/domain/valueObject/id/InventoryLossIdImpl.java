package com.minerva.domain.valueObject.id;

import java.util.Set;
import java.util.UUID;

import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.product.InventoryLossId;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;

public final class InventoryLossIdImpl extends ValueObject<UUID> implements InventoryLossId {

    public InventoryLossIdImpl(UUID value) throws NullValueException {
        super(value);
    }

    public static InventoryLossIdImpl fromString(String value) throws InvalidDomainArgumentException {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainArgumentException("El ID de la pérdida de inventario no puede estar vacío.");
        }

        try {
            return new InventoryLossIdImpl(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidDomainArgumentException(
                    "El ID de la pérdida de inventario no tiene un formato UUID válido.", e
            );
        }
    }

    public static InventoryLossIdImpl generate() {
        try {
            return new InventoryLossIdImpl(UUID.randomUUID());
        } catch (NullValueException e) {
            throw new UnexpectedDomainException(
                    "Error al generar el ID de pérdida de inventario: " + e.getMessage(),
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
        return "inventoryLossId";
    }

    @Override
    public String getAuditSubjectName() {
        return "inventoryLossId";
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
