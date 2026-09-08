package com.minerva.domain.entities.product;

import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.Observation;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.InventoryLossIdImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public class InventoryLoss extends Entity<InventoryLossId> {

    private final ProductId productId;
    private final ProductQuantity quantity;
    private InventoryLossReason reason;
    private Observation observation;
    private final LocalDateTime registrationDate;

    public InventoryLoss(
            ProductId productId,
            ProductQuantity quantity,
            InventoryLossReason reason,
            String observation
    ) throws DomainException {

        if (productId == null) throw new NullValueException("El nombre del producto no puede estar vacío.");
        if (quantity == null) throw new NullValueException("La cantidad debe ser mayor a cero.");
        if (quantity.isZeroOrLess()) throw new MinimumAmountException("La cantidad debe ser mayor a cero.");
        if (reason == null) throw new NullValueException("Debe especificar la razón de la pérdida.");

        super(InventoryLossIdImpl.generate());

        this.productId = productId;
        this.quantity = quantity;
        this.reason = reason;
        this.observation = observation == null
                ? null
                : new Observation(observation);
        this.registrationDate = LocalDateTime.now();
    }

    public ProductId getProductId() {
        return productId;
    }

    public ProductQuantity getQuantity() {
        return quantity;
    }

    public Optional<Observation> getObservation() {
        return Optional.ofNullable(observation);
    }

    public InventoryLossReason getReason() {
        return reason;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new StringAttribute(productId),
                new NumericAttribute("quantity", quantity),
                new StringAttribute(reason),
                new StringAttribute(observation),
                new StringAttribute("registrationDate", registrationDate)
        );
    }
}
