package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.id.ProductReturnIdImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Falta el id de la venta, pero creo que lo voya a cotnrolar desde la entidad sale
class ProductReturn extends Entity<ProductReturnId> {
    private final ProductQuantity quantity;
    private final ProductReturnReason reason;
    private final LocalDateTime registrationDate;

    public ProductReturn(ProductQuantity quantity, ProductReturnReason reason) throws DomainException {
        if (quantity != null && quantity.isZeroOrLess()) throw new MinimumAmountException("La cantidad a devolver debe ser mayor a cero.");
        if (reason == null) throw new NullValueException("La razón de la devolución no puede estar vacío.");

        super(ProductReturnIdImpl.generate());

        this.quantity = quantity;
        this.reason = reason;
        this.registrationDate = LocalDateTime.now();
    }

    public ProductQuantity getQuantity() {
        return quantity;
    }

    public ProductReturnReason getReason() {
        return reason;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Map<String, Attribute<?>> getAttributes() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "productReturnId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "quantity",
                quantity
        );

        attributes.put(
                "reason",
                reason
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
