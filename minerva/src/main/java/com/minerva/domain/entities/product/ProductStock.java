package com.minerva.domain.entities.product;

import com.minerva.domain.constants.SaleType;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.ProductQuantity;

import java.util.Optional;

public class ProductStock {

    private final SaleType saleType;
    private ProductQuantity quantity;
    //Puede ser null
    private ProductQuantity reorderLevel;
    //-----------------------------------

    public ProductStock(SaleType saleType, ProductQuantity stock, ProductQuantity reorderLevel) throws DomainException {

        if (saleType == null)
            throw new NullValueException("Seleccione el tipo de venta.");

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



    private ProductQuantity validateQuantity(ProductQuantity quantity, String field) throws DomainException {

        if (quantity == null)
            throw new NullValueException(field + " no puede ser nulo.");

        if (SaleType.UNIDAD.equals(saleType) && quantity.isDecimal()) {
            throw new DomainException(
                    field + " debe ser una cantidad entera para productos vendidos por unidad."
            );
        }

        return quantity;
    }

    public ProductQuantity getStock() {
        return quantity;
    }

    public Optional<ProductQuantity> getReorderLevel() {
        return Optional.ofNullable(reorderLevel);
    }

    public SaleType getSaleType() {
        return saleType;
    }
}
