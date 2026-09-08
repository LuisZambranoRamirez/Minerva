package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.UnexpectedDomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money extends ValueObject<BigDecimal> {
    public static final BigDecimal MIN_AMOUNT = BigDecimal.ZERO;
    public static final int MAX_DECIMALS = 2;

    /**
     * Construye una instancia de dinero validando los límites de precisión y monto mínimo.
     * <p>
     * Este constructor actúa como guardián del dominio, impidiendo la creación de importes
     * monetarios inválidos. Valida estrictamente que el número de decimales no exceda el límite
     * configurado y que el valor no sea inferior al fondo mínimo permitido (cero).
     * </p>
     *
     * @param value El importe numérico en formato {@link BigDecimal}.
     * @throws MinimumAmountException Si el monto es inferior a {@link #MIN_AMOUNT}.
     */
    public Money(BigDecimal value) throws InvalidDomainArgumentException {
        super(value);

        if (value.scale() > MAX_DECIMALS) throw new InvalidDomainArgumentException("El monto solo puede tener " + MAX_DECIMALS + " decimales.");
        if (value.compareTo(MIN_AMOUNT) < 0) throw new MinimumAmountException(MIN_AMOUNT.toString());
    }

    public boolean isGreaterThanZero() {
        return getValue().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isLessThanZero() {
        return getValue().compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return getValue().compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isZeroOrLess() {
        return getValue().compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isZeroOrGreater() {
        return getValue().compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isLessThan(Money other) {
        return getValue().compareTo(other.getValue()) < 0;
    }

    public boolean isGreaterThan(Money other) {
        return getValue().compareTo(other.getValue()) > 0;
    }

    public Money add(Money other) {
        try {
            return new Money(getValue().add(other.getValue()));
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, recenle al de arriba
            throw new UnexpectedDomainException("Error al sumar montos: " + e.getMessage(), e);
        }
    }

    public Money subtract(Money other) throws MinimumAmountException {
        try {
            return new Money(getValue().subtract(other.getValue()));
        } catch (MinimumAmountException e) {
            throw e;
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, récenle al de arriba
            throw new UnexpectedDomainException("Error al restar montos: " + e.getMessage(), e);
        }
    }

    /**
     * Incrementa el valor actual de este dinero aplicando un porcentaje.
     *
     * <p>Por ejemplo, si el monto es {@code 200} y el porcentaje es {@code 15%},
     * el resultado es {@code 230}.</p>
     *
     * @param percentage valor nominal del porcentaje a incrementar (ej: 15 para 15%)
     * @return una nueva instancia de {@link Money} con el monto incrementado
     * @throws InvalidDomainArgumentException si el porcentaje no es válido
     */
    public Money incrementPercentage(BigDecimal percentage) throws InvalidDomainArgumentException {
        Percentage percentageVo = new Percentage(percentage);

        BigDecimal percentageAmount = percentageVo.calculatePercentageOf(getValue());
        BigDecimal newAmount = getValue()
                .add(percentageAmount)
                .setScale(MAX_DECIMALS, RoundingMode.HALF_UP);

        try {
            return new Money(newAmount);
        } catch (InvalidDomainArgumentException e) {
            throw new UnexpectedDomainException("Error al incrementar el dinero en cierto porcentaje", e);
        }
    }

    public static Money zero() {
        try {
            return new Money(BigDecimal.ZERO);
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, récenle al de arriba
            throw new UnexpectedDomainException("Error al crear el monto cero.", e);
        }
    }

    public static Money tenCents() {
        try {
            return new Money(new BigDecimal("0.10"));
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, récenle al de arriba
            throw new UnexpectedDomainException("Error al crear el monto mínimo.", e);
        }
    }
}

