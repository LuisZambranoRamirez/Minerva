package com.minerva.domain.entities.sale;

import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class SaleDetail extends Entity<SaleDetailId> {
    private final ProductQuantity quantity;
    private final Money unitPrice;

    public SaleDetail(ProductQuantity quantity, Money unitPrice) throws DomainException {

        if (quantity != null && quantity.isZeroOrLess()) throw new DomainException("La CANTIDAD DE PRODUCTO debe ser mayor a 0.");
        if (unitPrice != null && unitPrice.isZeroOrLess()) throw new DomainException("El PRECIO UNITARIO debe ser mayor a 0.");

        super(SaleDetailIdImpl.generate());

        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public SaleDetail(UUID saleDetailId, BigDecimal quantity, BigDecimal unitPrice) {
        SaleDetailIdImpl tempId;
        try {
            tempId = new SaleDetailIdImpl(saleDetailId);
            this.quantity = new ProductQuantity(quantity);
            this.unitPrice = new Money(unitPrice);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear el detalle de venta: " + e.getMessage(), e);
        }
        super(tempId);
    }

    // REVISAR ESTO QUE YA ME GANO EL SUEÑO, si este mensaje sigue aqui es porque no lo revisé.
    public Money calculateSubTotal() {
        try {
            return new Money(unitPrice.getValue().multiply(quantity.getValue()));
        } catch (DomainException e) {
            // Si esto truena, recenle al de arriba
            throw new UnexpectedDomainException("Error al calcular el subtotal del detalle de venta: " + e.getMessage(), e);
        }
    }

    public ProductQuantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    @Override
    public Map<String, Attribute<?>> extractAuditData() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "saleDetailId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "quantity",
                quantity
        );

        attributes.put(
                "unitPrice",
                unitPrice
        );

        return attributes;
    }
}
