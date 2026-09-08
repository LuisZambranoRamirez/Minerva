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

    public Sale(UUID customerId, List<SaleItemWriteDTO> items) throws DomainException {
        super(SaleIdImpl.generate());
        this.customerId = new CustomerIdImpl(customerId);
        this.addSaleItem(items);
        this.registrationDate = LocalDateTime.now();
    }

    public Sale(UUID saleId, UUID customerId, LocalDateTime registrationDate, List<SaleItemReadDTO> saleItemReadDTOS, List<PayReadDTO> payReadDTOList) {
        SaleId saleIdValue;

        try {
            if (registrationDate == null) throw new InvalidDomainArgumentException("La fecha de registro no puede ser nula");
            if (saleItemReadDTOS == null) throw new InvalidDomainArgumentException("La lista de items de venta no puede ser nula");
            if (payReadDTOList == null) throw new InvalidDomainArgumentException("La lista de pagos no puede ser nula");

            if (saleItemReadDTOS.stream().anyMatch(Objects::isNull)) throw new InvalidDomainArgumentException("La lista de items de venta no puede contener elementos nulos");
            if (payReadDTOList.stream().anyMatch(Objects::isNull)) throw new InvalidDomainArgumentException("La lista de pagos no puede contener elementos nulos");

            saleIdValue = new SaleIdImpl(saleId);
            this.customerId = new CustomerIdImpl(customerId);
            this.registrationDate = registrationDate;
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException(e.getMessage(), e);
        }
        super(saleIdValue);

        for (SaleItemReadDTO saleItemReadDTO : saleItemReadDTOS) {
            SaleDetail saleDetail = new SaleDetail(
                    saleItemReadDTO.saleDetailId.getIdValue(),
                    saleItemReadDTO.productQuantity.getValue(),
                    saleItemReadDTO.unitPrice.getValue()
            );
            this.saleDetails.put(saleItemReadDTO.productId, saleDetail);
        }

        for (PayReadDTO payReadDTO : payReadDTOList) {
            Pay pay = new Pay(
                payReadDTO.payId.getIdValue(),
                payReadDTO.amount.getValue(),
                payReadDTO.paymentMethod,
                payReadDTO.registrationDate
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

    public record SaleItemWriteDTO(ProductSale productSale, BigDecimal quantity, BigDecimal unitPrice) {}
    public record SaleItemReadDTO(SaleDetailId saleDetailId, SaleId saleId, ProductId productId, ProductQuantity productQuantity, Money unitPrice) {}
    public record PayWriteDTO(BigDecimal amount, PaymentMethod paymentMethod) {}
    public record PayReadDTO(PayId payId, SaleId saleId, Money amount, PaymentMethod paymentMethod, LocalDateTime registrationDate) {}

    // nota: se deberia poner un minimo de ganancia sobre el costo cuando se negocia con el cliente el precio, por el momento solo se mira si es menor que el costo
    private void addSaleItem(List<SaleItemWriteDTO> items) throws DomainException {
        if (items == null) throw new NullValueException("La venta debe tener al menos un item");
        if (items.isEmpty()) throw new DomainException("La venta debe tener al menos un item");

        for (SaleItemWriteDTO item : items) {
            ProductSale productSale = item.productSale();

            if (saleDetails.containsKey(productSale.getId())) {
                throw new DomainException(
                        "El producto ya existe en la venta. Modifique la cantidad en lugar de agregarlo nuevamente."
                );
            }

            Money unitPriceMoney;

            if (item.unitPrice() == null) {
                unitPriceMoney = productSale.getPrice();
            } else {
                unitPriceMoney = new Money(item.unitPrice());

                if (unitPriceMoney.isLessThan(productSale.getCost())) {
                    throw new DomainException(
                            "El precio de venta no puede ser menor que el costo."
                    );
                }
            }

            SaleDetail newDetail = new SaleDetail(
                    new ProductQuantity(item.quantity()),
                    unitPriceMoney
            );

            saleDetails.put(productSale.getId(), newDetail);
        }
    }

    public List<SaleItemReadDTO> getSaleDetails() {
        List<SaleItemReadDTO> saleItemReadDTOList = new ArrayList<>(saleDetails.size());

        saleDetails.forEach((productId, saleDetail) -> saleItemReadDTOList.add(
                new SaleItemReadDTO(
                        saleDetail.getId(),
                        this.getId(),
                        productId,
                        saleDetail.getQuantity(),
                        saleDetail.getUnitPrice())));

        return saleItemReadDTOList;
    }

    public Result<Void> addPayment(PayWriteDTO payWriteDTO) {
        if (isDueCanceled()) return Result.fail("La VENTA ya esta CANCELADA");

        Pay payCreated;
        try {
            payCreated = new Pay(new Money(payWriteDTO.amount), payWriteDTO.paymentMethod);
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
                            pay.getId(),
                            this.getId(),
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

    public boolean isDueCanceled() {
        return calculateAmountDue().isZero();
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
}
