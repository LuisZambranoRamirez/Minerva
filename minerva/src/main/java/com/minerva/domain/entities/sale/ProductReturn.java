package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.valueObject.id.ProductReturnIdImpl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class ProductReturn extends Entity<ProductReturnId> {
    private final SaleDetailId saleDetailId;
    private final ProductQuantity quantity;
    private final ProductReturnReason reason;
    private final LocalDateTime registrationDate;

    public ProductReturn(SaleDetailId saleDetailId, ProductQuantity quantity,
                         ProductReturnReason reason) throws DomainException {
        super(ProductReturnIdImpl.generate());
        LocalDateTime now = LocalDateTime.now();
        validate(saleDetailId, quantity, reason, now);
        this.saleDetailId = saleDetailId;
        this.quantity = quantity;
        this.reason = reason;
        this.registrationDate = now;
    }

    public ProductReturn(ProductReturnId id, SaleDetailId saleDetailId,
                         ProductQuantity quantity, ProductReturnReason reason,
                         LocalDateTime registrationDate) {
        super(id);
        try {
            validate(saleDetailId, quantity, reason, registrationDate);
            this.saleDetailId = saleDetailId;
            this.quantity = quantity;
            this.reason = reason;
            this.registrationDate = registrationDate;
        } catch (DomainException exception) {
            throw new EntityRestoreException("Error al restaurar la devolución: " + exception.getMessage(), exception);
        }
    }

    private static void validate(SaleDetailId saleDetailId, ProductQuantity quantity,
                                 ProductReturnReason reason, LocalDateTime registrationDate)
            throws DomainException {
        if (saleDetailId == null) throw new NullValueException("El detalle de venta no puede estar vacío.");
        if (quantity == null) throw new NullValueException("La cantidad a devolver no puede estar vacía.");
        if (quantity.isZeroOrLess()) throw new MinimumAmountException("La cantidad a devolver debe ser mayor a cero.");
        if (reason == null) throw new NullValueException("La razón de la devolución no puede estar vacía.");
        if (registrationDate == null) throw new NullValueException("La fecha de la devolución no puede estar vacía.");
    }

    public boolean restoresStock() { return reason == ProductReturnReason.EQUIVOCACION; }
    public SaleDetailId getSaleDetailId() { return saleDetailId; }
    public ProductQuantity getQuantity() { return quantity; }
    public ProductReturnReason getReason() { return reason; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute((Id<?>) getId()));
        attributes.add(new StringAttribute((Id<?>) saleDetailId));
        attributes.add(new NumericAttribute("quantity", quantity));
        attributes.add(new StringAttribute(reason));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        return Set.copyOf(attributes);
    }
}
