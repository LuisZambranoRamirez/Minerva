package com.minerva.domain.entities.stockEntry;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.product.StockEntryProduct;
import com.minerva.domain.entities.stockReceipt.StockReceiptId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.valueObject.id.StockEntryIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public final class StockEntry extends Entity<StockEntryId> implements StockEntryProduct {

    private final StockReceiptId stockReceiptId;
    private final ProductId productId;
    private final Money unitPrice;
    private final ProductQuantity quantity;
    private final LocalDateTime expirationDate;

    public StockEntry(
            StockReceiptId stockReceiptId,
            ProductId productId,
            BigDecimal unitPrice,
            BigDecimal quantity,
            LocalDateTime expirationDate
    ) throws DomainException {
        this(
                StockEntryIdImpl.generate(),
                stockReceiptId,
                productId,
                new Money(unitPrice),
                new ProductQuantity(quantity),
                expirationDate
        );
    }

    public StockEntry(
            StockEntryId id,
            StockReceiptId stockReceiptId,
            ProductId productId,
            Money unitPrice,
            ProductQuantity quantity,
            LocalDateTime expirationDate
    ) throws DomainException {
        super(id);

        if (stockReceiptId == null) throw new NullValueException("La recepción no puede ser nula.");
        if (productId == null) throw new NullValueException("El producto no puede ser nulo.");
        if (unitPrice == null || unitPrice.isZeroOrLess()) throw new DomainException("El precio del producto debe ser mayor a cero.");
        if (quantity == null || quantity.isZeroOrLess()) throw new DomainException("La cantidad del producto debe ser mayor a cero.");
        if (expirationDate != null && !expirationDate.isAfter(LocalDateTime.now())) {
            throw new DomainException("La fecha de expiración debe ser posterior a la fecha actual.");
        }

        this.stockReceiptId = stockReceiptId;
        this.productId = productId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }

    public StockReceiptId getStockReceiptId() {
        return stockReceiptId;
    }

    @Override
    public ProductId getProductId() {
        return productId;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    @Override
    public ProductQuantity getQuantity() {
        return quantity;
    }

    public Optional<LocalDateTime> getExpirationDate() {
        return Optional.ofNullable(expirationDate);
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new java.util.HashSet<>();
        attributes.add(new StringAttribute((Id<?>) getId()));
        attributes.add(new StringAttribute((Id<?>) stockReceiptId));
        attributes.add(new StringAttribute((Id<?>) productId));
        attributes.add(new NumericAttribute("unitPrice", unitPrice));
        attributes.add(new NumericAttribute("quantity", quantity));
        if (expirationDate != null) {
            attributes.add(new StringAttribute("expirationDate", expirationDate));
        }
        return Set.copyOf(attributes);
    }
}
