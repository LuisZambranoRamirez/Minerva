package com.minerva.domain.valueObject;

import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;

import java.math.BigDecimal;

public class Markup extends ValueObject<Markup.Value> {

    public Markup(BigDecimal amount, GainStrategy gainStrategy) throws InvalidDomainArgumentException {
        if (gainStrategy == null) throw new NullValueException("Seleccione una estrategia de ganancia.");
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidDomainArgumentException("El valor de la ganancia debe ser mayor que cero.");
        super(new Value(amount, gainStrategy));
    }

    public Money apply(Money cost) throws InvalidDomainArgumentException {
        return switch (getValue().gainStrategy) {
            case GainStrategy.RECARGO_FIJO -> cost.add(new Money(getValue().amount));
            case GainStrategy.PORCENTAJE -> cost.incrementPercentage(getValue().amount);
        };
    }

    public record Value(
            BigDecimal amount,
            GainStrategy gainStrategy
    ) {}
}
