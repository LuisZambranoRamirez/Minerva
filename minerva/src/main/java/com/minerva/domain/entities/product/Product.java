package com.minerva.domain.entities.product;

import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.Modifier;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.sale.ProductSale;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.BarCode;
import com.minerva.domain.valueObject.Markup;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductName;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.ProductStock;
import com.minerva.domain.valueObject.SKU;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.valueObject.id.ProductIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Product extends Entity<ProductId> implements ProductSale {

    private final SKU sku;
    private final ProductName productName;
    private final Markup markup;
    private final ProductStock productStock;
    private final BarCode barCode;
    private Money cost;
    private final ProductCategory productCategory;
    private final LocalDateTime registrationDate;

    private final Map<Modifier, Money> modifiers = new HashMap<>();
    private final Map<Product, ProductQuantity> bulkProducts = new HashMap<>();

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

        if (productCategory == null) {
            throw new NullValueException("Seleccione una categoría.");
        }

        this.sku = new SKU(sku);
        this.productName = new ProductName(productName);
        this.productStock = new ProductStock(
                saleType,
                new ProductQuantity(initialStock),
                reorderLevel == null ? null : new ProductQuantity(reorderLevel)
        );
        this.markup = new Markup(gainAmount, gainStrategy);
        this.productCategory = productCategory;
        this.cost = new Money(purchasePrice);
        this.registrationDate = LocalDateTime.now();
        this.barCode = createBarCode(barCode, saleType);
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
            if (productCategory == null || registrationDate == null) {
                throw new NullValueException("Los datos persistidos del producto están incompletos.");
            }

            this.sku = sku;
            this.productName = productName;
            this.productStock = new ProductStock(
                    saleType,
                    stock,
                    reorderLevel == null ? null : new ProductQuantity(reorderLevel)
            );
            this.markup = new Markup(gainAmount, gainStrategy);
            this.barCode = createBarCode(barCode, saleType);
            this.productCategory = productCategory;
            this.cost = cost;
            this.registrationDate = registrationDate;
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el producto: " + e.getMessage(), e);
        }
    }

    private BarCode createBarCode(String value, SaleType saleType) throws DomainException {
        if (value == null || value.isBlank()) {
            if (SaleType.UNIDAD.equals(saleType)) {
                throw new DomainException("Ingrese el código de barras para productos vendidos por unidad.");
            }
            return null;
        }

        return new BarCode(value);
    }

    public Result<Void> addModifier(Modifier modifier, Money price) {
        if (modifier == null) return Result.fail("El modificador no puede ser nulo.");
        if (price == null) return Result.fail("El precio del modificador no puede ser nulo.");
        modifiers.put(modifier, price);
        return Result.success(null);
    }

    public Optional<Money> getModifierPrice(Modifier modifier) {
        return Optional.ofNullable(modifiers.get(modifier));
    }

    public Result<Void> processDeliveryFromSupplier(StockEntryProduct stockEntryProduct) {
        if (stockEntryProduct == null) return Result.fail("La entrada de stock no puede ser nula.");
        if (!getId().equals(stockEntryProduct.getProductId())) return Result.fail("La entrada de stock no corresponde a este producto.");
        return productStock.increase(stockEntryProduct.getQuantity());
    }

    public Result<Void> processSale(ProductSale productSale) {
        if (productSale == null) return Result.fail("La venta del producto no puede ser nula.");
        return Result.fail("La cantidad vendida debe ser proporcionada por el detalle de venta.");
    }

    public Result<Void> decreaseStock(ProductQuantity quantity) {
        return productStock.decrease(quantity);
    }

    public Result<Void> increaseStock(ProductQuantity quantity) {
        return productStock.increase(quantity);
    }

    public Result<Void> addBulkAssociation(Product bulkProduct, ProductQuantity quantity) {
        if (bulkProduct == null) return Result.fail("El producto a granel no puede ser nulo.");
        if (quantity == null || quantity.isZeroOrLess()) return Result.fail("La cantidad debe ser mayor a cero.");
        if (equals(bulkProduct)) return Result.fail("No es posible asociar un producto consigo mismo.");
        if (getSaleType() != SaleType.UNIDAD) return Result.fail("El producto de origen debe venderse por unidad.");
        if (bulkProduct.getSaleType() != SaleType.GRANEL) return Result.fail("El producto asociado debe venderse a granel.");

        bulkProducts.put(bulkProduct, quantity);
        return Result.success(null);
    }

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
        return productStock.getQuantity();
    }

    public Optional<ProductQuantity> getReorderLevel() {
        return productStock.getReorderLevel();
    }

    public GainStrategy getGainStrategy() {
        return markup.getValue().gainStrategy();
    }

    public SaleType getSaleType() {
        return productStock.getSaleType();
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
        } catch (DomainException e) {
            throw new UnexpectedDomainException(e.getMessage(), e);
        }
    }

    @Override
    public Money getPrice() {
        return calculatePrice();
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute((Id<?>) getId()));
        attributes.add(new StringAttribute(sku));
        attributes.add(new StringAttribute(productName));
        attributes.add(new NumericAttribute("stock", getStock()));
        attributes.add(new StringAttribute(getGainStrategy()));
        attributes.add(new NumericAttribute("gainAmount", getGainAmount()));
        getReorderLevel().ifPresent(value -> attributes.add(new NumericAttribute("reorderLevel", value)));
        getBarCode().ifPresent(value -> attributes.add(new StringAttribute(value)));
        attributes.add(new StringAttribute(getSaleType()));
        attributes.add(new NumericAttribute("cost", cost));
        attributes.add(new NumericAttribute("price", calculatePrice()));
        attributes.add(new StringAttribute(productCategory));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        return Set.copyOf(attributes);
    }
}
