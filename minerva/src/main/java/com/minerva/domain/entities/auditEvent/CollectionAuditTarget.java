package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.valueObject.id.Id;

import java.util.Set;

/**
 * Representa una colección completa como objetivo de auditoría.
 *
 * Se utiliza cuando una operación no apunta a una entidad individual, por
 * ejemplo al consultar todos los productos o todas las ventas.
 */
public final class CollectionAuditTarget implements Id<String> {

    private static final String ALL = "ALL";

    public enum Resource {
        PRODUCTS,
        INVENTORY_LOSSES,
        CUSTOMERS,
        SUPPLIERS,
        SALES,
        PRODUCT_RETURNS,
        ORDERS,
        ORDER_TRANSITIONS,
        INVENTORY_MOVEMENTS,
        LOW_STOCK_PRODUCTS
    }

    private final Resource resource;

    public CollectionAuditTarget(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("El recurso de auditoría no puede ser nulo.");
        }

        this.resource = resource;
    }

    @Override
    public String getIdValue() {
        return ALL;
    }

    @Override
    public String getIdValueAsString() {
        return ALL;
    }

    @Override
    public String getIdName() {
        return "collectionScope";
    }

    @Override
    public String getAuditSubjectName() {
        return resource.name();
    }

    @Override
    public Id<?> getAuditSubjectId() {
        return this;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute("resource", resource.name()),
                new StringAttribute("scope", ALL)
        );
    }
}
