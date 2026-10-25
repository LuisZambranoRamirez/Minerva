package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.valueObject.id.Id;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;

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
        INVENTORY_REPORTS,
        LOW_STOCK_PRODUCTS
    }

    private final Resource resource;
    private final Map<String, String> metadata;

    public CollectionAuditTarget(Resource resource) {
        this(resource, Map.of());
    }

    public CollectionAuditTarget(Resource resource, Map<String, String> metadata) {
        if (resource == null) {
            throw new IllegalArgumentException("El recurso de auditoría no puede ser nulo.");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Los metadatos de auditoría no pueden ser nulos.");
        }

        this.resource = resource;
        this.metadata = Map.copyOf(metadata);
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
        Set<Attribute<?>> attributes = new LinkedHashSet<>();
        attributes.add(new StringAttribute("resource", resource.name()));
        attributes.add(new StringAttribute("scope", ALL));
        metadata.forEach((name, value) -> attributes.add(new StringAttribute(name, value)));
        return Set.copyOf(attributes);
    }
}
