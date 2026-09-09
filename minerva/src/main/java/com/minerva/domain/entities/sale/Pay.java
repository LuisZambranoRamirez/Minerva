package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.PaymentMethod;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.id.PayIdImpl;

import java.time.LocalDateTime;
import java.util.Set;

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

    Pay(PayId payId, Money amount, PaymentMethod paymentMethod, LocalDateTime registrationDate) {
        super(payId);
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.registrationDate = registrationDate;
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
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new NumericAttribute("amount", amount),
                new StringAttribute(paymentMethod),
                new StringAttribute("registrationDate", registrationDate)
        );
    }
}
