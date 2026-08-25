package com.minerva.domain.entities.stockEntry;

import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.product.StockEntryProduct;
import com.minerva.domain.entities.supplier.SupplierId;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.SupplierName;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.id.StockEntryIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StockEntry extends Entity<StockEntryId> implements StockEntryProduct {

    private final ProductId productId;
    private final SupplierId supplierId;
    private final Money unitPrice;
    private final ProductQuantity quantity;
    // PUedes ser null
    private final LocalDateTime expirationDate;
    // --------------------
    private final LocalDateTime registrationDate;

    public StockEntry(
            UUID productId,
            String supplierName,
            BigDecimal unitPrice,
            BigDecimal quantity,
            LocalDateTime expirationDate
    ) throws DomainException {        

        this.productId = new ProductIdImpl(productId);
        this.supplierId = new SupplierName(supplierName);
        this.unitPrice = new Money(unitPrice);
        this.quantity = new ProductQuantity(quantity);
        this.expirationDate = expirationDate;
        this.registrationDate = LocalDateTime.now();
        super(StockEntryIdImpl.generate());

        if (this.unitPrice.isZeroOrLess()) throw new DomainException("El precio del producto debe ser mayor a 0.");
        if (this.quantity.isZeroOrLess()) throw new DomainException("La cantidad del producto debe ser mayor a 0.");
        if (expirationDate != null &&(expirationDate.isBefore(LocalDateTime.now()) || expirationDate.isEqual(LocalDateTime.now()))) {
            throw new DomainException("La fecha de expiración debe ser posterior a la fecha actual.");
        }
    }

    public StockEntry(
            UUID stockEntryId,
            UUID productId,
            String supplierName,
            BigDecimal unitPrice,
            BigDecimal quantity,
            LocalDateTime expirationDate,
            LocalDateTime registrationDate
    ) {
        StockEntryIdImpl tempId;
        try {
            tempId = new StockEntryIdImpl(stockEntryId);
            this.productId = new ProductIdImpl(productId);
            this.supplierId = new SupplierName(supplierName);
            this.unitPrice = new Money(unitPrice);
            this.quantity = new ProductQuantity(quantity);
            this.expirationDate = expirationDate;
            this.registrationDate = registrationDate;
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear la entrada de stock: " + e.getMessage(), e);
        }
        super(tempId);
    }

    @Override
    public ProductId getProductId() {
        return productId;
    }

    public SupplierId getSupplierId() {
        return supplierId;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Optional<LocalDateTime> getExpirationDate() {
        return Optional.ofNullable(expirationDate);
    }

    @Override
    public ProductQuantity getQuantity() {
        return quantity;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Map<String, Attribute<?>> extractAuditData() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "stockEntryId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "productId",
                new DefaultStringAttribute(productId.asString())
        );

        attributes.put(
                "supplierId",
                new DefaultStringAttribute(supplierId.asString())
        );

        attributes.put(
                "unitPrice",
                unitPrice
        );

        attributes.put(
                "quantity",
                quantity
        );

        attributes.put(
                "expirationDate",
                new DefaultDateTimeAttribute(expirationDate)
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
