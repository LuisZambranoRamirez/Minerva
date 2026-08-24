package com.minerva.domain.entities.product;

import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultNumericAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.services.Result;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.sale.ProductSale;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.valueObject.*;
import com.minerva.domain.valueObject.id.ProductIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.minerva.domain.services.Math.isDecimal;
import static com.minerva.domain.services.Math.isZeroOrLess;

public class Product extends Entity<ProductId> implements ProductSale {
    private final ProductName productName;
    private ProductQuantity stock;
    private Markup markup;
    //Puede ser null
    private ProductQuantity reorderLevel;
    private final BarCode barCode;
    //----------------------------------------------
    private SaleType saleType;
    private Money cost;
    private final ProductCategory productCategory;
    private final LocalDateTime registrationDate;

    public Product(
            String productName,
            GainStrategy gainStrategy,
            BigDecimal gainAmount,
            BigDecimal reorderLevel,
            String barCode,
            SaleType saleType,
            BigDecimal initialStock,
            ProductCategory productCategory,
            BigDecimal purchasePrice
    ) throws DomainException {

        if (saleType == null) throw new NullValueException("Seleccione el tipo de venta.");
        if (productCategory == null) throw new NullValueException("Seleccione una categoría.");

        if (reorderLevel != null) {
            if (SaleType.UNIDAD.equals(saleType) && isDecimal(reorderLevel))
                throw new DomainException("El nivel de reposición no puede ser decimal para productos vendidos por unidad.");

            this.reorderLevel = new ProductQuantity(reorderLevel);
        }

        if (barCode == null) {
            if (SaleType.UNIDAD.equals(saleType))
                throw new DomainException("Ingrese el código de barras para productos vendidos por unidad.");
            this.barCode = null;
        } else {
            this.barCode = new BarCode(barCode);
        }

        super(ProductIdImpl.generate());
        this.productName = new ProductName(productName);
        this.stock = new ProductQuantity(initialStock);
        this.markup = new Markup(gainAmount, gainStrategy);
        this.saleType = saleType;
        this.productCategory = productCategory;
        this.cost = new Money(purchasePrice);
        this.registrationDate = LocalDateTime.now();
    }

    public Product(
            UUID productId,
            String productName,
            GainStrategy gainStrategy,
            BigDecimal gainAmount,
            BigDecimal reorderLevel,
            String barCode,
            SaleType saleType,
            BigDecimal stock,
            ProductCategory productCategory,
            BigDecimal cost,
            LocalDateTime registrationDate
    ) {
        ProductId tempId;
        try {
            if (saleType == null) throw new NullValueException("Seleccione el tipo de venta.");
            if (productCategory == null) throw new NullValueException("Seleccione una categoría.");
            if (registrationDate == null) throw new NullValueException("Seleccione la fecha de registro.");

            tempId = new ProductIdImpl(productId);
            this.productName = new ProductName(productName);
            this.stock = new ProductQuantity(stock);
            this.markup = new Markup(gainAmount, gainStrategy);
            this.saleType = saleType;
            this.productCategory = productCategory;
            this.reorderLevel = reorderLevel == null ? null : new ProductQuantity(reorderLevel);
            this.barCode = barCode == null ? null : new BarCode(barCode);
            this.cost = new Money(cost);
            this.registrationDate = registrationDate;

        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear el producto: " + e.getMessage(), e);
        }
        super(tempId);
    }

    // --------------------------------

    public Result<Void> processDeliveryFromSupplier(StockEntryProduct stockEntryProduct) {
        if (stockEntryProduct == null) return Result.fail("La entrada de stock no puede ser nula.");
        if (!getId().equals(stockEntryProduct.getProductId())) return Result.fail("La entrada de stock no corresponde a este producto.");
        return increaseStock(stockEntryProduct.getQuantity());
    }

    public Result<Void> processSale(SaleProduct saleProduct) {
        if (saleProduct == null) return Result.fail("La venta del producto no puede ser nula");

        Optional<ProductQuantity> soldQuantity = saleProduct.getSoldQuantity(this.getId());
        if (soldQuantity.isEmpty()) return Result.fail("El producto no existe en la venta");
        return decreaseStock(soldQuantity.get());
    }

    //----------------------------------

    private Result<Void> increaseStock(ProductQuantity quantityToAdd) {
        ProductQuantity newStockValue = this.stock.add(quantityToAdd);
        return updateStock(newStockValue);
    }

    private Result<Void> decreaseStock(ProductQuantity quantityToSubtract) {
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

    private Result<Void> updateStock(ProductQuantity newStockValue) {
        if (newStockValue == null)
            return Result.fail("El nuevo valor de stock no puede ser nulo.");

        if (this.saleType == SaleType.UNIDAD && newStockValue.isDecimal())
            return Result.fail("Este producto se maneja por unidades. Ingrese una cantidad entera.");
  
        this.stock = newStockValue;
        return Result.success(null);  
    }

    // -----------------------------------------------------
    public Result<Void> validateBulkAssociation(Product bulkProduct, ProductQuantity quantity) {
        if (bulkProduct == null) return Result.fail("El producto a granel no puede ser nulo.");
        if (quantity == null) return Result.fail("La cantidad no puede estar vacío");

        if (this.equals(bulkProduct)) return Result.fail("No es posible asociar un producto consigo mismo.");
        if (this.getSaleType() != SaleType.UNIDAD ) return Result.fail("El producto -- " + this.getNameId() + " -- se vende por unidad y no permite asociar otro producto.");

        if (bulkProduct.getSaleType() != SaleType.GRANEL) return Result.fail("El producto -- " + bulkProduct.getNameId() + " -- debe venderse a granel para poder ser asociado.");
        if (quantity.isZeroOrLess()) return Result.fail("La cantidad debe ser mayor a cero");

        return Result.success(null);
    }

    // ---------------------------------------------

    public ProductName getNameId() {
        return productName;
    }

    public Optional<BarCode> getBarCode() {
        return Optional.ofNullable(barCode);
    }

    public BigDecimal getGainAmount() {
        return markup.getValue().amount();
    }

    public ProductQuantity getStock() {
        return stock;
    }

    public Optional<ProductQuantity> getReorderLevel() {
        return Optional.ofNullable(reorderLevel);
    }

    public GainStrategy getGainStrategy() {
        return markup.getValue().gainStrategy();
    }

    public SaleType getSaleType() {
        return saleType;
    }

    public ProductCategory getCategory() {
        return productCategory;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Money getCost() {
        return cost;
    }

    public Money calculatePrice() {
        try {
            return markup.apply(cost);
        } catch (InvalidDomainArgumentException e) {
            throw new UnexpectedDomainException(e.getMessage(), e);
        }
    }

    @Override
    public Money getPrice() {
        return calculatePrice();
    }

    // falta el sku
    @Override
    public Map<String, Attribute<?>> getAttributes() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "productId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "productName",
                productName
        );

        attributes.put(
                "stock",
                stock
        );

        attributes.put(
                "gainStrategy",
                getGainStrategy()
        );

        attributes.put(
                "gainAmount",
                new DefaultNumericAttribute(getGainAmount())
        );

        attributes.put(
                "reorderLevel",
                reorderLevel
        );

        attributes.put(
                "barCode",
                barCode
        );

        attributes.put(
                "saleType",
                saleType
        );
        // NO esta en el esquema de la db
        attributes.put(
                "cost",
                cost
        );

        attributes.put(
                "price",
                calculatePrice()
        );
        // ----------------
        attributes.put(
                "productCategory",
                productCategory
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
