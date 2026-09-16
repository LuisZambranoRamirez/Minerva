package com.minerva.domain.valueObject;

import com.minerva.domain.constants.SaleType;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.MinimumAmountException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.services.Result;

import java.util.Optional;

public class ProductStock extends ValueObject<ProductStock.Value> {

    public record Value(
            SaleType saleType,
            ProductQuantity quantity,
            //Puede ser null
            ProductQuantity reorderLevel
            // -----------------------------------
    ) {}

    public ProductStock(SaleType saleType, ProductQuantity stock, ProductQuantity reorderLevel) throws DomainException {
        if (saleType == null)
            throw new NullValueException("Seleccione el tipo de venta.");

        super(new Value(saleType, stock, reorderLevel));

        this.saleType = saleType;
        this.quantity = validateQuantity(stock, "El stock");
        this.reorderLevel = reorderLevel == null ? null : validateQuantity(reorderLevel, "El nivel de reposición");

        if (reorderLevel != null) {
            this.reorderLevel = new ProductQuantity(reorderLevel);
            if (SaleType.UNIDAD.equals(saleType) && this.reorderLevel.isDecimal())
                throw new DomainException("El nivel de reposición no puede ser decimal para productos vendidos por unidad.");
        }
    }

    public Result<Void> increaseStock(ProductQuantity quantity) {
        ProductQuantity newStock = quantity.add(quantity);
        return updateStock(newStock);
    }

    private Result<Void> updateStock(ProductQuantity newStock) {
        if (saleType == SaleType.UNIDAD && newStock.isDecimal()) {
            return Result.fail(
                    "Este producto se maneja por unidades. " +
                            "Ingrese una cantidad entera."
            );
        }

        this.quantity = newStock;
        return Result.success(null);
    }

    private Result<Void> updateStock(ProductQuantity newStockValue) {
        if (newStockValue == null)
            return Result.fail("El nuevo valor de stock no puede ser nulo.");

        if (SaleType.UNIDAD.equals(saleType) && newStockValue.isDecimal())
            return Result.fail("Este producto se maneja por unidades. Ingrese una cantidad entera.");

        this.stock = newStockValue;
        return Result.success(null);
    }



    private Result<Void> increaseStock(ProductQuantity quantityToAdd) {
        ProductQuantity newStockValue = this.stock.add(quantityToAdd);
        return updateStock(newStockValue);
    }

    private Result<Void> decreaseStock(ProductQuantity quantityToSubtract) {
        if (quantityToSubtract == null) return Result.fail("La cantidad a descontar no puede ser nula.");
        if (this.stock.isZero()) return Result.fail("No hay stock disponible para este producto.");

        try {
            ProductQuantity newStockValue = this.stock.subtract(quantityToSubtract);
            if (newStockValue.isLessThanZero())
                return Result.fail(
                        "No hay suficiente stock para realizar la operación. " +
                                "Stock disponible: " + this.stock.getValue() +
                                ". Cantidad solicitada: " + quantityToSubtract.getValue() + "."
                );
            return updateStock(newStockValue);
        } catch (MinimumAmountException e) {
            return Result.fail(e.getMessage());
        }
    }

    public ProductQuantity getStock() {
        return getValue().quantity;
    }

    public Optional<ProductQuantity> getReorderLevel() {
        return Optional.ofNullable(getValue().reorderLevel);
    }

    public SaleType getSaleType() {
        return getValue().saleType;
    }
}
