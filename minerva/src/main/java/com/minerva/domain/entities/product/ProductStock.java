package com.minerva.domain.entities.product;

import com.minerva.domain.constants.SaleType;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.ValueObject;

import java.util.Optional;

public class ProductStock extends ValueObject<ProductStock.Value> {

    public record Value(
            ProductQuantity stock,
            SaleType saleType,
            Optional<ProductQuantity> reorderLevel
    ) {
    }

    public ProductStock(ProductQuantity stock, SaleType saleType, ProductQuantity reorderLevel) throws DomainException {
        super(new ProductStock.Value(stock, saleType, Optional.ofNullable(reorderLevel)));

        if (saleType == null)
            throw new NullValueException("Seleccione el tipo de venta.");

        Result<Void> quantityResult = validateQuantity(stock);
        if (quantityResult.isFail())
            throw new DomainException(quantityResult.getMessage());


        if (reorderLevel != null) {
            Result<Void> reorderLevelResult = validateQuantity(reorderLevel);

            if (reorderLevelResult.isFail())
                throw new DomainException(reorderLevelResult.getMessage());
        }
    }

    public Result<ProductStock> increaseStock(ProductQuantity quantityToAdd) {
        ProductQuantity newStockValue = getValue().stock().add(quantityToAdd);

        try {
            ProductStock newProductStock = new ProductStock(
                    newStockValue,
                    getValue().saleType(),
                    getValue().reorderLevel().orElse(null)
            );

            return Result.success(newProductStock);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    // que no sea negativo se controla en la clase productquantity
    public Result<ProductStock> decreaseStock(ProductQuantity quantityToSubtract) {
        if (quantityToSubtract == null)
            return Result.fail("La cantidad a descontar no puede ser nula.");

        if (getValue().stock().isZero())
            return Result.fail("No hay stock disponible para este producto.");

        try {
            ProductQuantity newStockValue =
                    getValue().stock().subtract(quantityToSubtract);

            ProductStock newProductStock = new ProductStock(
                    newStockValue,
                    getValue().saleType(),
                    getValue().reorderLevel().orElse(null)
            );

            return Result.success(newProductStock);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    private Result<Void> validateQuantity(ProductQuantity quantity) {
        if (quantity == null)
            return Result.fail("El nuevo valor de stock no puede ser nulo.");

        if (SaleType.UNIDAD.equals(getValue().saleType) && quantity.isDecimal())
            return Result.fail("Este producto se maneja por unidades. Ingrese una cantidad entera.");

        return Result.success(null);
    }

    public ProductQuantity getStock() {
        return getValue().stock;
    }

    public SaleType getSaleType() {
        return getValue().saleType;
    }

    public Optional<ProductQuantity> getReorderLevel() {
        return getValue().reorderLevel;
    }
}
