package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.MinimumAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Percentage extends ValueObject<BigDecimal> {
    private static final BigDecimal MIN_VALUE = BigDecimal.ZERO;


    /**
     * Crea un porcentaje a partir de su valor nominal.
     *
     * <p>El valor debe ser mayor o igual a cero. Por ejemplo:
     * {@code 15} representa {@code 15%} y {@code 12.5} representa {@code 12.5%}.</p>
     *
     * @param value valor nominal del porcentaje.
     * @throws MinimumAmountException si el valor es inferior a cero.
     */
    public Percentage(BigDecimal value) throws InvalidDomainArgumentException {
        super(value);
        if (value.compareTo(MIN_VALUE) < 0) throw new MinimumAmountException(MIN_VALUE.toString());
    }

    /**
     * Calcula este porcentaje sobre un valor.
     *
     * <p>Fórmula: {@code valor * (porcentaje / 100)}.
     * Por ejemplo, {@code 15%} de {@code 200} devuelve {@code 30}.</p>
     *
     * @param value valor sobre el cual se calcula el porcentaje
     * @return el valor correspondiente a este porcentaje
     */
    public BigDecimal calculatePercentageOf(BigDecimal value) {
        return value
                .multiply(getValue())
                .divide(BigDecimal.valueOf(100), value.scale(), RoundingMode.HALF_UP);
    }
}
