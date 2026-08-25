package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.PaymentMethod;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.id.PayIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Pay extends Entity<PayId> {
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final LocalDateTime registrationDate;

    private static final Money MIN_AMOUNT = Money.tenCents();

    Pay(Money amount, PaymentMethod paymentMethod) throws DomainException {
        if (paymentMethod == null) throw new NullValueException("El método de pago no puede estar vacío.");
        if (amount != null && amount.isLessThan(MIN_AMOUNT)) throw new MinimumAmountException("El MONTO debe ser mayor o igual a S/" + MIN_AMOUNT);

        super(PayIdImpl.generate());
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.registrationDate = LocalDateTime.now();
    }

    Pay(UUID payId, BigDecimal amount, PaymentMethod paymentMethod, LocalDateTime registrationDate) {
        PayIdImpl tempId;
        try {
            if (paymentMethod == null) throw new InvalidDomainArgumentException("El método de pago no puede ser nulo");
            if (registrationDate == null) throw new InvalidDomainArgumentException("La fecha de registro no puede ser nula");

            tempId = new PayIdImpl(payId);
            this.amount = new Money(amount);
            this.paymentMethod = paymentMethod;
            this.registrationDate = registrationDate;
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear el pago: " + e.getMessage(), e);
        }
        super(tempId);
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Map<String, Attribute<?>> extractAuditData() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "payId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "amount",
                amount
        );

        attributes.put(
                "paymentMethod",
                paymentMethod
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
