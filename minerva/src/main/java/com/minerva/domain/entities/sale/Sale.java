package com.minerva.domain.entities.sale;

import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.ArrayAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.product.SaleProduct;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.services.Result;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.constants.PaymentMethod;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.SaleIdImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Sale extends Entity<SaleId> implements SaleProduct {
    private final CustomerId customerId;
    private final LocalDateTime registrationDate;

    private final List<Pay> pays =  new LinkedList<>();
    private final Map<ProductId, SaleDetail> saleDetails = new HashMap<>();

    public Sale(UUID customerId, List<SaleDetailCreateDTO> items) throws DomainException {
        super(SaleIdImpl.generate());
        this.customerId = new CustomerIdImpl(customerId);
        this.addSaleDetail(items);
        this.registrationDate = LocalDateTime.now();
    }

    public Sale(SaleId saleId, CustomerId customerId, LocalDateTime registrationDate, List<SaleDetailRestoreDTO> saleItemRestoreDTOList, List<PayRestoreDTO> payRestoreDTOS) {
        super(saleId);
        this.customerId = customerId;
        this.registrationDate = registrationDate;

        for (SaleDetailRestoreDTO saleDetailRestoreDTO : saleItemRestoreDTOList) {
            if (this.getId() != saleDetailRestoreDTO.saleId) throw new EntityRestoreException("El ID de la venta no coincide con el ID de los detalles de venta.");

            SaleDetail saleDetail = new SaleDetail(
                    this.getId(),
                    saleDetailRestoreDTO.saleDetailId,
                    saleDetailRestoreDTO.productId,
                    saleDetailRestoreDTO.quantity,
                    saleDetailRestoreDTO.unitPrice
            );

            this.saleDetails.put(saleDetail.getProductId(), saleDetail);
        }

        for (PayRestoreDTO payRestoreDTO : payRestoreDTOS) {
            if (this.getId() != payRestoreDTO.saleId) throw new EntityRestoreException("El ID de la venta no coincide con el ID de los pagos.");

            Pay pay = new Pay(
                this.getId(),
                payRestoreDTO.payId,
                payRestoreDTO.amount,
                payRestoreDTO.paymentMethod,
                payRestoreDTO.registrationDate
            );

            this.pays.add(pay);
        }
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();

        attributes.add(new StringAttribute(getId()));
        attributes.add(new StringAttribute(customerId));
        attributes.add(new NumericAttribute("total", calculateTotal()));
        attributes.add(new NumericAttribute("totalPaid", calculateTotalPaid()));
        attributes.add(new NumericAttribute("amountDue", calculateAmountDue()));
        attributes.add(new StringAttribute("registration_date", registrationDate));

        List<Set<Attribute<?>>> saleDetails = this.saleDetails
                .values()
                .stream()
                .map(SaleDetail::getAuditData)
                .toList();

        attributes.add(new ArrayAttribute("saleDetails", saleDetails));

        List<Set<Attribute<?>>> pays = this.pays
                .stream()
                .map(Pay::getAuditData)
                .toList();

        attributes.add(new ArrayAttribute("pays", pays));

        return attributes;
    }

    @Override
    public Optional<ProductQuantity> getSoldQuantity(ProductId productId) {
        SaleDetail saleDetail = saleDetails.get(productId);
        if (saleDetail == null) return Optional.empty();
        return Optional.of(saleDetail.getQuantity());
    }

    public record SaleDetailCreateDTO(ProductSale productSale, BigDecimal quantity, BigDecimal unitPrice) {}
    public record SaleDetailReadDTO(SaleId saleId, SaleDetailId saleDetailId, ProductId productId, ProductQuantity productQuantity, Money unitPrice) {}
    public record SaleDetailRestoreDTO(SaleId saleId, SaleDetailId saleDetailId, ProductId productId, ProductQuantity quantity, Money unitPrice) {}

    public record PayCreateDTO(BigDecimal amount, PaymentMethod paymentMethod) {}
    public record PayReadDTO(SaleId saleId, PayId payId, Money amount, PaymentMethod paymentMethod, LocalDateTime registrationDate) {}
    public record PayRestoreDTO(SaleId saleId, PayId payId, Money amount, PaymentMethod paymentMethod, LocalDateTime registrationDate) {}

    // nota: se deberia poner un minimo de ganancia sobre el costo cuando se negocia con el cliente el precio, por el momento solo se mira si es menor que el costo
    private void addSaleDetail(List<SaleDetailCreateDTO> items) throws DomainException {
        if (items == null) throw new NullValueException("La venta debe tener al menos un item");
        if (items.isEmpty()) throw new DomainException("La venta debe tener al menos un item");

        for (SaleDetailCreateDTO item : items) {
            ProductSale productSale = item.productSale();

            if (saleDetails.containsKey(productSale.getId()))
                throw new DomainException("El producto ya existe en la venta. Modifique la cantidad en lugar de agregarlo nuevamente.");


            Money unitPriceMoney;

            if (item.unitPrice() == null) {
                unitPriceMoney = productSale.getPrice();
            } else {
                unitPriceMoney = new Money(item.unitPrice());

                if (unitPriceMoney.isLessThan(productSale.getCost()))
                    throw new DomainException("El precio de venta no puede ser menor que el costo.");
            }

            SaleDetail newDetail = new SaleDetail(
                    this.getId(),
                    productSale.getId(),
                    new ProductQuantity(item.quantity),
                    unitPriceMoney
            );

            saleDetails.put(productSale.getId(), newDetail);
        }
    }

    public List<SaleDetailReadDTO> getSaleDetails() {
        List<SaleDetailReadDTO> saleDetailReadDTOList = new ArrayList<>(saleDetails.size());

        saleDetails.forEach((productId, saleDetail) -> saleDetailReadDTOList.add(
                new SaleDetailReadDTO(
                        this.getId(),
                        saleDetail.getId(),
                        productId,
                        saleDetail.getQuantity(),
                        saleDetail.getUnitPrice())));

        return saleDetailReadDTOList;
    }

    public Result<Void> addPayment(PayCreateDTO payCreateDTO) {
        if (payCreateDTO == null) return Result.fail("El pago es obligatorio.");
        if (isFullyPaid()) return Result.fail("La venta ya está completamente pagada.");

        Pay payCreated;
        try {
            payCreated = new Pay(this.getId(), new Money(payCreateDTO.amount), payCreateDTO.paymentMethod);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
        
        if (payCreated.getAmount().isGreaterThan(calculateAmountDue()))
            return Result.fail("El PAGO sobrepasa la DEUDA de la VENTA.");

        pays.add(payCreated);
        return Result.success(null);
    }

    public List<PayReadDTO> getPays() {
        List<PayReadDTO> payReadDTOList = new ArrayList<>(pays.size());

        for (Pay pay : pays) {
            payReadDTOList.add(
                    new PayReadDTO(
                            this.getId(),
                            pay.getId(),
                            pay.getAmount(),
                            pay.getPaymentMethod(),
                            pay.getRegistrationDate()
                    ));
        }

        return payReadDTOList;
    }

    public Money calculateTotal() {
        return saleDetails.values().stream()
                .map(SaleDetail::calculateSubTotal)
                .reduce(Money.zero(), Money::add);
    }

    public Money calculateTotalPaid() {
        return pays.stream()
                .map(Pay::getAmount)
                .reduce(Money.zero(), Money::add);
    }

    public Money calculateAmountDue() {
        try {
            return calculateTotal().subtract(calculateTotalPaid());
        } catch (DomainException e) {
            throw new UnexpectedDomainException("Error al calcular el monto adeudado: " + e.getMessage(), e);
        }
    }

    public boolean isFullyPaid() {
        return calculateAmountDue().isZero();
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
}
