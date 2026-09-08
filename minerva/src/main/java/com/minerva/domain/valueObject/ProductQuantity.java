package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.UnexpectedDomainException;

import java.math.BigDecimal;

public class ProductQuantity extends ValueObject<BigDecimal> {
    // DECIMAL(10,3)
    private static final BigDecimal MIN_AMOUNT = BigDecimal.ZERO;
    private static final int MAX_DECIMALS = 3;

    public ProductQuantity(BigDecimal value) throws InvalidDomainArgumentException {
        super(value);

        if (value.scale() > MAX_DECIMALS) throw new InvalidDomainArgumentException("La cantidad no puede tener decimales.");
        if (value.compareTo(MIN_AMOUNT) < 0) throw new MinimumAmountException(MIN_AMOUNT.toString());
    }

    // Nota: Mejorar el nombre
    public static ProductQuantity zero() {
        try {
            return new ProductQuantity(BigDecimal.ZERO);
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, récenle al de arriba
            throw new UnexpectedDomainException("Error al crear la cantidad cero.", e);
        }
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

// --------------------- COMPARACIONES ---------------------BORRAR SI NO TIENE USO

    public boolean isGreaterThan(ProductQuantity other) {
        return getValue().compareTo(other.getValue()) > 0;
    }

    public boolean isLessThan(ProductQuantity other) {
        return getValue().compareTo(other.getValue()) < 0;
    }

    public ProductQuantity add(ProductQuantity other) {
        try {
            return new ProductQuantity(getValue().add(other.getValue()));
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, récenle al de arriba
            throw new UnexpectedDomainException("Error al sumar cantidades de producto: " + e.getMessage(), e);
        }
    }

    public ProductQuantity subtract(ProductQuantity other) throws MinimumAmountException {
        try {
            return new ProductQuantity(getValue().subtract(other.getValue()));
        } catch (MinimumAmountException e) {
            throw e;
        } catch (InvalidDomainArgumentException e) {
            // Si esto truena, récenle al de arriba
            throw new UnexpectedDomainException("Error al restar cantidades de producto: " + e.getMessage(), e);
        }
    }

    public boolean isDecimal() {
        return getValue().scale() > 0;
    }

    public boolean isInteger() {
        return getValue().scale() == 0;
    }

}
