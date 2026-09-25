package com.minerva.domain.entities.inventory;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.InventoryMovementIdImpl;
import com.minerva.domain.valueObject.id.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InventoryMovement extends Entity<InventoryMovementId> {
    private final ProductId productId;
    private final BigDecimal quantity;
    private final ProductQuantity stockBefore;
    private final ProductQuantity stockAfter;
    private final InventoryMovementType type;
    private final InventoryMovementSource source;
    private final UUID sourceId;
    private final UserId actorId;
    private final LocalDateTime registrationDate;

    public InventoryMovement(
            ProductId productId,
            BigDecimal quantity,
            ProductQuantity stockBefore,
            ProductQuantity stockAfter,
            InventoryMovementSource source,
            UUID sourceId,
            UserId actorId
    ) throws DomainException {
        this(
                InventoryMovementIdImpl.generate(),
                productId,
                quantity,
                stockBefore,
                stockAfter,
                resolveType(quantity),
                source,
                sourceId,
                actorId,
                LocalDateTime.now()
        );
    }

    public InventoryMovement(
            InventoryMovementId inventoryMovementId,
            ProductId productId,
            BigDecimal quantity,
            ProductQuantity stockBefore,
            ProductQuantity stockAfter,
            InventoryMovementType type,
            InventoryMovementSource source,
            UUID sourceId,
            UserId actorId,
            LocalDateTime registrationDate
    ) throws DomainException {
        super(inventoryMovementId);
        validate(productId, quantity, stockBefore, stockAfter, type, source, actorId, registrationDate);
        this.productId = productId;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.type = type;
        this.source = source;
        this.sourceId = sourceId;
        this.actorId = actorId;
        this.registrationDate = registrationDate;
    }

    private static void validate(
            ProductId productId,
            BigDecimal quantity,
            ProductQuantity stockBefore,
            ProductQuantity stockAfter,
            InventoryMovementType type,
            InventoryMovementSource source,
            UserId actorId,
            LocalDateTime registrationDate
    ) throws DomainException {
        if (productId == null) throw new DomainException("El producto del movimiento no puede estar vacío.");
        if (quantity == null) throw new DomainException("La cantidad del movimiento no puede estar vacía.");
        if (quantity.compareTo(BigDecimal.ZERO) == 0) throw new DomainException("La cantidad del movimiento no puede ser cero.");
        if (quantity.scale() > 3) throw new DomainException("La cantidad del movimiento no puede tener más de 3 decimales.");
        if (stockBefore == null) throw new DomainException("El stock anterior del movimiento no puede estar vacío.");
        if (stockAfter == null) throw new DomainException("El stock posterior del movimiento no puede estar vacío.");
        if (type == null) throw new DomainException("El tipo del movimiento no puede estar vacío.");
        if (source == null) throw new DomainException("El origen del movimiento no puede estar vacío.");
        if (actorId == null) throw new DomainException("El actor del movimiento no puede estar vacío.");
        if (registrationDate == null) throw new DomainException("La fecha del movimiento no puede estar vacía.");
        if (type == InventoryMovementType.ENTRADA && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Los movimientos de entrada deben tener cantidad positiva.");
        }
        if (type == InventoryMovementType.SALIDA && quantity.compareTo(BigDecimal.ZERO) >= 0) {
            throw new DomainException("Los movimientos de salida deben tener cantidad negativa.");
        }
    }

    private static InventoryMovementType resolveType(BigDecimal quantity) throws DomainException {
        if (quantity == null) throw new DomainException("La cantidad del movimiento no puede estar vacía.");
        if (quantity.compareTo(BigDecimal.ZERO) > 0) return InventoryMovementType.ENTRADA;
        if (quantity.compareTo(BigDecimal.ZERO) < 0) return InventoryMovementType.SALIDA;
        throw new DomainException("La cantidad del movimiento no puede ser cero.");
    }

    public ProductId getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ProductQuantity getStockBefore() {
        return stockBefore;
    }

    public ProductQuantity getStockAfter() {
        return stockAfter;
    }

    public InventoryMovementType getType() {
        return type;
    }

    public InventoryMovementSource getSource() {
        return source;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public UserId getActorId() {
        return actorId;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute(getId()));
        attributes.add(new StringAttribute(productId));
        attributes.add(new NumericAttribute("quantity", quantity));
        attributes.add(new NumericAttribute("stockBefore", stockBefore));
        attributes.add(new NumericAttribute("stockAfter", stockAfter));
        attributes.add(new StringAttribute("type", type.name()));
        attributes.add(new StringAttribute("source", source.name()));
        if (sourceId != null) attributes.add(new StringAttribute("sourceId", sourceId.toString()));
        attributes.add(new StringAttribute((Id<?>) actorId));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        return Set.copyOf(attributes);
    }
}