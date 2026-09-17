package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.Modifier;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;

import java.util.*;

class SaleDetail extends Entity<SaleDetailId> {
    private final SaleId saleId;
    private final ProductId productId;
    private final ProductQuantity quantity;
    private final Money unitPrice;
    private final Map<Modifier, Money> modifiers;

    public SaleDetail(SaleId saleId, ProductId productId, ProductQuantity quantity, Money unitPrice, Map<Modifier, Money> modifiers) throws DomainException {
        if (saleId == null) throw new UnexpectedDomainException("El saleId es nulo");
        if (productId == null) throw new NullValueException("Debe seleccionar un producto.");
        if (quantity != null && quantity.isZeroOrLess()) throw new DomainException("La CANTIDAD DE PRODUCTO debe ser mayor a 0.");
        if (unitPrice != null && unitPrice.isZeroOrLess()) throw new DomainException("El PRECIO UNITARIO debe ser mayor a 0.");
        if (modifiers == null)
            throw new NullValueException("Los modificadores no pueden ser nulos.");

        for (Map.Entry<Modifier, Money> entry : modifiers.entrySet()) {
            if (entry.getKey() == null)
                throw new NullValueException("El modificador no puede ser nulo.");

            if (entry.getValue() == null)
                throw new NullValueException("El precio del modificador no puede ser nulo.");
        }

        super(SaleDetailIdImpl.generate());
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.modifiers = modifiers;
    }

    public SaleDetail(SaleId saleId, SaleDetailId saleDetailId, ProductId productId, ProductQuantity quantity, Money unitPrice, Map<Modifier, Money> modifiers) {
        super(saleDetailId);
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.modifiers = modifiers;
    }


    // REVISAR ESTO QUE YA ME GANO EL SUEÑO, si este mensaje sigue aqui es porque no lo revisé.
    // a la hora de caclular subtotal para los productos que se venden a granel evaluar como regla de negocio que se auemtne el extra por
    // kilo, por el momento simplemente se agregara el precio extra idependiemtemente de la cantida de kilos.
    public Money calculateSubTotal() {
        try {
            // Precio del producto * cantidad
            Money subtotal = new Money(
                    unitPrice.getValue().multiply(quantity.getValue())
            );

            // Sumar el precio extra de cada modifier
            for (Money modifierPrice : modifiers.values()) {
                subtotal = subtotal.add(modifierPrice);
            }

            return subtotal;
        } catch (DomainException e) {
            // Si esto truena, recenle al de arriba
            throw new UnexpectedDomainException("Error al calcular el subtotal del detalle de venta: " + e.getMessage(), e);
        }
    }

    public SaleId getSaleId() {
        return saleId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public ProductQuantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Map<Modifier, Money> getModifiers() {
        return new HashMap<>(modifiers);
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
