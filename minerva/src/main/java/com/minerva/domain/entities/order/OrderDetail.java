package com.minerva.domain.entities.order;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.id.OrderDetailIdImpl;

import java.util.Set;

public class OrderDetail extends Entity<OrderDetailId> {
    private final ProductId productId;
    private final ProductQuantity quantity;
    private final Money unitPrice;

    public OrderDetail(ProductId productId, ProductQuantity quantity, Money unitPrice) throws DomainException {
        super(OrderDetailIdImpl.generate());
        validate(productId, quantity, unitPrice);
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public OrderDetail(OrderDetailId orderDetailId, ProductId productId, ProductQuantity quantity, Money unitPrice) {
        super(orderDetailId);
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    private static void validate(ProductId productId, ProductQuantity quantity, Money unitPrice) throws DomainException {
        if (productId == null) throw new DomainException("El producto del pedido no puede estar vacío.");
        if (quantity == null) throw new DomainException("La cantidad del pedido no puede estar vacía.");
        if (quantity.isZeroOrLess()) throw new DomainException("La cantidad del pedido debe ser mayor a 0.");
        if (unitPrice == null) throw new DomainException("El precio unitario del pedido no puede estar vacío.");
        if (unitPrice.isZeroOrLess()) throw new DomainException("El precio unitario del pedido debe ser mayor a 0.");
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

    public Money calculateSubTotal() {
        try {
            return new Money(unitPrice.getValue().multiply(quantity.getValue()));
        } catch (DomainException e) {
            throw new UnexpectedDomainException("Error al calcular el subtotal del detalle de pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new StringAttribute(productId),
                new NumericAttribute("quantity", quantity),
                new NumericAttribute("unitPrice", unitPrice)
        );
    }
}