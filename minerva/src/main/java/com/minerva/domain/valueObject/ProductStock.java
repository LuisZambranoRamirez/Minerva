package com.minerva.domain.valueObject;

import com.minerva.domain.constants.SaleType;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.services.Result;

import java.util.Optional;

/**
 * Encapsula el stock y sus invariantes según el tipo de venta del producto.
 */
public final class ProductStock {

    private final SaleType saleType;
    private ProductQuantity quantity;
    private final ProductQuantity reorderLevel;

    public ProductStock(SaleType saleType, ProductQuantity quantity, ProductQuantity reorderLevel) throws DomainException {
        if (saleType == null) {
            throw new NullValueException("Seleccione el tipo de venta.");
        }
        if (quantity == null) {
            throw new NullValueException("El stock no puede ser nulo.");
        }
        if (SaleType.UNIDAD.equals(saleType) && quantity.isDecimal()) {
            throw new DomainException("El stock no puede ser decimal para productos vendidos por unidad.");
        }
        if (reorderLevel != null && SaleType.UNIDAD.equals(saleType) && reorderLevel.isDecimal()) {
            throw new DomainException("El nivel de reposición no puede ser decimal para productos vendidos por unidad.");
        }

        this.saleType = saleType;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    public Result<Void> increase(ProductQuantity quantityToAdd) {
        if (quantityToAdd == null || quantityToAdd.isZeroOrLess()) {
            return Result.fail("La cantidad a agregar debe ser mayor a cero.");
        }

        return update(quantity.add(quantityToAdd));
    }

    public Result<Void> decrease(ProductQuantity quantityToSubtract) {
        if (quantityToSubtract == null || quantityToSubtract.isZeroOrLess()) {
            return Result.fail("La cantidad a descontar debe ser mayor a cero.");
        }
        if (quantity.isZero()) {
            return Result.fail("No hay stock disponible para este producto.");
        }

        try {
            return update(quantity.subtract(quantityToSubtract));
        } catch (MinimumAmountException e) {
            return Result.fail("No hay suficiente stock para realizar la operación.");
        }
    }

    private Result<Void> update(ProductQuantity newQuantity) {
        if (SaleType.UNIDAD.equals(saleType) && newQuantity.isDecimal()) {
            return Result.fail("Este producto se maneja por unidades. Ingrese una cantidad entera.");
        }

        quantity = newQuantity;
        return Result.success(null);
    }

    public ProductQuantity getQuantity() {
        return quantity;
    }

    public Optional<ProductQuantity> getReorderLevel() {
        return Optional.ofNullable(reorderLevel);
    }

    public SaleType getSaleType() {
        return saleType;
    }
}
