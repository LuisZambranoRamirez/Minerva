package com.minerva.domain.entities.stockReceipt;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.supplier.SupplierId;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.valueObject.id.StockReceiptIdImpl;

import java.time.LocalDateTime;
import java.util.Set;

public final class StockReceipt extends Entity<StockReceiptId> {

    private final SupplierId supplierId;
    private final LocalDateTime registrationDate;

    public StockReceipt(SupplierId supplierId) throws NullValueException {
        this(StockReceiptIdImpl.generate(), supplierId, LocalDateTime.now());
    }

    public StockReceipt(StockReceiptId id, SupplierId supplierId, LocalDateTime registrationDate) throws NullValueException {
        super(id);

        if (supplierId == null) {
            throw new NullValueException("El proveedor de la recepción no puede ser nulo.");
        }
        if (registrationDate == null) {
            throw new NullValueException("La fecha de recepción no puede ser nula.");
        }

        this.supplierId = supplierId;
        this.registrationDate = registrationDate;
    }

    public SupplierId getSupplierId() {
        return supplierId;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute((Id<?>) getId()),
                new StringAttribute((Id<?>) supplierId),
                new StringAttribute("registrationDate", registrationDate)
        );
    }
}
