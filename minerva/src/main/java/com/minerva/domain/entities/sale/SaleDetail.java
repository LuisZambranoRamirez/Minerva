package com.minerva.domain.entities.sale;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;

import java.util.Set;

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

    public SaleDetail(SaleDetailId saleDetailId, ProductQuantity quantity, Money unitPrice) {
        super(saleDetailId);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
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
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new NumericAttribute("quantity", quantity),
                new NumericAttribute("unitPrice", unitPrice)
        );
    }
}
