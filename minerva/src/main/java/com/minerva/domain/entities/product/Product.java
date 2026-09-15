package com.minerva.domain.entities.product;

import com.minerva.domain.constants.Modifier;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.services.Result;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.sale.ProductSale;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.valueObject.*;
import com.minerva.domain.valueObject.id.ProductIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Product extends Entity<ProductId> implements ProductSale {
    private final SKU sku;
    private final ProductName productName;
    private Markup markup;
    private final ProductStock productStock;
    //Puede ser null
    private final BarCode barCode;
    //----------------------------------------------
    private Money cost;
    private final ProductCategory productCategory;
    private final LocalDateTime registrationDate;

    // TABLAS
    private final Map<Modifier, Money> modifiers = new HashMap<>();
    private final Map<Product, ProductQuantity> bulkProducts = new HashMap<>();
    // -------------------------

    public Result<Void> addModifier(Modifier modifier, Money price) {
        if (modifier == null) return Result.fail("El modificador no puede ser nulo.");
        if (price == null) return Result.fail("El precio del modificador no puede ser nulo.");

        modifiers.put(modifier, price);
        return Result.success(null);
    }

    @Override
    public Optional<Money> getModifierPrice(Modifier modifier) {
        return Optional.ofNullable(modifiers.get(modifier));
    }

    public Product(
            String sku,
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

        super(ProductIdImpl.generate());
        this.sku = new SKU(sku);
        this.productName = new ProductName(productName);
        this.productStock = new ProductStock(saleType, new ProductQuantity(initialStock), new ProductQuantity(reorderLevel));
        this.markup = new Markup(gainAmount, gainStrategy);
        this.productCategory = productCategory;
        this.cost = new Money(purchasePrice);
        this.registrationDate = LocalDateTime.now();

        if (barCode == null) {
            if (SaleType.UNIDAD.equals(saleType))
                throw new DomainException("Ingrese el código de barras para productos vendidos por unidad.");
            this.barCode = null;
        } else {
            this.barCode = new BarCode(barCode);
        }
    }

    public Product(
            ProductId productId,
            SKU sku,
            ProductName productName,
            GainStrategy gainStrategy,
            BigDecimal gainAmount,
            BigDecimal reorderLevel,
            String barCode,
            SaleType saleType,
            ProductQuantity stock,
            ProductCategory productCategory,
            Money cost,
            LocalDateTime registrationDate
    ) {
        super(productId);
        try {
            this.sku = sku;
            this.productName = productName;
            this.stock = stock;
            this.markup = new Markup(gainAmount, gainStrategy);
            this.saleType = saleType;
            this.productCategory = productCategory;
            this.reorderLevel = reorderLevel == null ? null : new ProductQuantity(reorderLevel);
            this.barCode = barCode == null ? null : new BarCode(barCode);
            this.cost = cost;
            this.registrationDate = registrationDate;
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear el producto: " + e.getMessage(), e);
        }
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

    private Result<Void> updateStock(ProductQuantity newStockValue) {
        if (newStockValue == null)
            return Result.fail("El nuevo valor de stock no puede ser nulo.");

        if (SaleType.UNIDAD.equals(saleType) && newStockValue.isDecimal())
            return Result.fail("Este producto se maneja por unidades. Ingrese una cantidad entera.");
  
        this.stock = newStockValue;
        return Result.success(null);  
    }

    // -----------------------------------------------------
    public Result<Void> addBulkAssociation(Product bulkProduct, ProductQuantity quantity) {
        if (bulkProduct == null) return Result.fail("El producto a granel no puede ser nulo.");
        if (quantity == null) return Result.fail("La cantidad no puede estar vacío");

        if (this.equals(bulkProduct)) return Result.fail("No es posible asociar un producto consigo mismo.");
        if (saleType != SaleType.UNIDAD ) return Result.fail("El producto -- " + getProductName() + " -- se vende por unidad y no permite asociar otro producto.");

        if (bulkProduct.getSaleType() != SaleType.GRANEL) return Result.fail("El producto -- " + bulkProduct.getProductName() + " -- debe venderse a granel para poder ser asociado.");
        if (quantity.isZeroOrLess()) return Result.fail("La cantidad debe ser mayor a cero");

        bulkProducts.put(bulkProduct, quantity);
        return Result.success(null);
    }

    // ---------------------------------------------


    public SKU getSku() {
        return sku;
    }

    public ProductName getProductName() {
        return productName;
    }

    public Optional<BarCode> getBarCode() {
        return Optional.ofNullable(barCode);
    }

    public BigDecimal getGainAmount() {
        return markup.getValue().amount();
    }

    public ProductQuantity getStock() {
        return ProductStock;
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
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new StringAttribute(sku),
                new StringAttribute(productName),
                new NumericAttribute("stock", ProductStock),
                new StringAttribute(getGainStrategy()),
                new NumericAttribute("gainAmount", getGainAmount()),
                new NumericAttribute("reorderLevel", reorderLevel),
                new StringAttribute(barCode),
                new StringAttribute(saleType),
                new NumericAttribute("cost", cost),
                new NumericAttribute("price", calculatePrice()),
                new StringAttribute(productCategory),
                new StringAttribute("registrationDate", registrationDate)
        );
    }
}
