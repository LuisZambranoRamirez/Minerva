package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.entities.sale.SaleId;
import com.minerva.domain.entities.sale.ProductReturn;
import com.minerva.domain.entities.sale.SaleDetailId;
import com.minerva.domain.entities.order.OrderId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.repositories.SaleRepository;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.PayIdImpl;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;
import com.minerva.domain.valueObject.id.SaleIdImpl;
import com.minerva.domain.valueObject.id.ProductReturnIdImpl;
import com.minerva.domain.valueObject.id.OrderIdImpl;
import com.minerva.infrastructure.persistence.entity.CustomerEntity;
import com.minerva.infrastructure.persistence.entity.PayEntity;
import com.minerva.infrastructure.persistence.entity.ProductEntity;
import com.minerva.infrastructure.persistence.entity.SaleDetailEntity;
import com.minerva.infrastructure.persistence.entity.SaleEntity;
import com.minerva.infrastructure.persistence.entity.ProductReturnEntity;
import com.minerva.infrastructure.persistence.repository.JpaPayRepository;
import com.minerva.infrastructure.persistence.repository.JpaSaleDetailRepository;
import com.minerva.infrastructure.persistence.repository.JpaSaleRepository;
import com.minerva.infrastructure.persistence.repository.JpaProductReturnRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class SaleRepositoryAdapter implements SaleRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final JpaSaleRepository saleRepository;
    private final JpaPayRepository payRepository;
    private final JpaSaleDetailRepository saleDetailRepository;
    private final JpaProductReturnRepository productReturnRepository;

    public SaleRepositoryAdapter(JpaSaleRepository saleRepository, JpaPayRepository payRepository,
                                 JpaSaleDetailRepository saleDetailRepository,
                                 JpaProductReturnRepository productReturnRepository) {
        this.saleRepository = saleRepository;
        this.payRepository = payRepository;
        this.saleDetailRepository = saleDetailRepository;
        this.productReturnRepository = productReturnRepository;
    }

    @Override
    @Transactional
    public void save(Sale sale, Set<Product> products) {
        SaleEntity saleEntity = saleRepository.save(toEntity(sale));
        saveSaleDetails(sale.getSaleDetails(), saleEntity);
        savePayments(sale.getPays(), saleEntity);

        for (Product product : products) {
            ProductEntity productEntity = entityManager.find(ProductEntity.class, product.getId().getIdValue());
            if (productEntity == null) {
                throw new EntityRestoreException("No se encontró el producto al actualizar el stock de la venta.");
            }
            productEntity.setStock(product.getStock().getValue());
        }
    }

    @Override
    @Transactional
    public void updatePayments(Sale sale) {
        SaleEntity saleEntity = entityManager.getReference(SaleEntity.class, sale.getId().getIdValue());
        savePayments(sale.getPays(), saleEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sale> findById(SaleId saleId) {
        return saleRepository.findById(saleId.getIdValue()).map(this::toDomainWithRelations);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sale> findBySourceOrderId(OrderId orderId) {
        return saleRepository.findBySourceOrder_OrderId(orderId.getIdValue()).map(this::toDomainWithRelations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> findByCustomerId(CustomerId customerId) {
        return saleRepository.findByCustomer_CustomerId(customerId.getIdValue()).stream()
                .map(this::toDomainWithRelations)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> findAll() {
        List<SaleEntity> sales = saleRepository.findAll();
        Map<UUID, List<Sale.SaleItemRestoreDTO>> detailsBySale = saleDetailRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        detail -> detail.getSale().getSaleId(),
                        Collectors.mapping(this::toSaleItemRestoreDTO, Collectors.toList())
                ));
        Map<UUID, List<Sale.PayRestoreDTO>> paymentsBySale = payRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        payment -> payment.getSale().getSaleId(),
                        Collectors.mapping(this::toPayRestoreDTO, Collectors.toList())
                ));

        List<Sale> result = new ArrayList<>(sales.size());
        for (SaleEntity sale : sales) {
            result.add(toDomain(
                    sale,
                    detailsBySale.getOrDefault(sale.getSaleId(), List.of()),
                    paymentsBySale.getOrDefault(sale.getSaleId(), List.of())
            ));
        }
        return result;
    }

    @Override
    @Transactional
    public Optional<ReturnContext> findReturnContextForUpdate(SaleDetailId saleDetailId) {
        return saleDetailRepository.findByIdForProductReturn(saleDetailId.getIdValue()).map(detail -> {
            ProductQuantity returnedQuantity;
            try {
                java.math.BigDecimal totalReturned = productReturnRepository
                        .findBySaleDetail_SaleDetailId(detail.getSaleDetailId()).stream()
                        .map(ProductReturnEntity::getQuantity)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                returnedQuantity = new ProductQuantity(totalReturned);
                return new ReturnContext(
                        new ProductIdImpl(detail.getProduct().getProductId()),
                        new ProductQuantity(detail.getQuantity()),
                        returnedQuantity
                );
            } catch (DomainException exception) {
                throw new EntityRestoreException("Error al consultar las cantidades de la devolución.", exception);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSaleDetailById(SaleDetailId saleDetailId) {
        return saleDetailRepository.existsById(saleDetailId.getIdValue());
    }

    @Override
    @Transactional
    public void saveProductReturn(ProductReturn productReturn, Product product) {
        SaleDetailEntity detail = entityManager.getReference(
                SaleDetailEntity.class, productReturn.getSaleDetailId().getIdValue());
        ProductEntity productEntity = entityManager.find(ProductEntity.class, product.getId().getIdValue());
        if (productEntity == null) {
            throw new EntityRestoreException("No se encontró el producto de la devolución.");
        }
        if (productReturn.restoresStock()) productEntity.setStock(product.getStock().getValue());
        productReturnRepository.save(new ProductReturnEntity(
                productReturn.getId().getIdValue(), detail, productReturn.getQuantity().getValue(),
                productReturn.getReason(), productReturn.getRegistrationDate()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReturn> findProductReturnsBySaleDetailId(SaleDetailId saleDetailId) {
        return productReturnRepository.findBySaleDetail_SaleDetailId(saleDetailId.getIdValue())
                .stream().map(this::toProductReturnDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReturn> findAllProductReturns() {
        return productReturnRepository.findAll().stream().map(this::toProductReturnDomain).toList();
    }

    private void saveSaleDetails(List<Sale.SaleItemReadDTO> details, SaleEntity sale) {
        for (Sale.SaleItemReadDTO detail : details) {
            ProductEntity product = entityManager.getReference(ProductEntity.class, detail.productId().getIdValue());
            saleDetailRepository.save(new SaleDetailEntity(
                    detail.saleDetailId().getIdValue(),
                    sale,
                    product,
                    detail.productQuantity().getValue(),
                    detail.unitPrice().getValue()
            ));
        }
    }

    private void savePayments(List<Sale.PayReadDTO> payments, SaleEntity sale) {
        for (Sale.PayReadDTO payment : payments) {
            payRepository.save(new PayEntity(
                    payment.payId().getIdValue(),
                    sale,
                    payment.amount().getValue(),
                    payment.paymentMethod(),
                    payment.registrationDate()
            ));
        }
    }

    private Sale toDomainWithRelations(SaleEntity sale) {
        SaleId saleId = createSaleId(sale.getSaleId());
        List<Sale.SaleItemRestoreDTO> details = saleDetailRepository.findBySale_SaleId(sale.getSaleId())
                .stream().map(this::toSaleItemRestoreDTO).toList();
        List<Sale.PayRestoreDTO> payments = payRepository.findBySale_SaleId(sale.getSaleId())
                .stream().map(this::toPayRestoreDTO).toList();
        return toDomain(sale, saleId, details, payments);
    }

    private Sale toDomain(SaleEntity entity, List<Sale.SaleItemRestoreDTO> details,
                          List<Sale.PayRestoreDTO> payments) {
        return toDomain(entity, createSaleId(entity.getSaleId()), details, payments);
    }

    private Sale toDomain(SaleEntity entity, SaleId saleId, List<Sale.SaleItemRestoreDTO> details,
                          List<Sale.PayRestoreDTO> payments) {
        try {
            return new Sale(
                    saleId,
                    new CustomerIdImpl(entity.getCustomer().getCustomerId()),
                    entity.getRegistrationDate(),
                    details,
                    payments,
                    entity.getSourceOrder() == null ? null : new OrderIdImpl(entity.getSourceOrder().getOrderId())
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar la venta.", e);
        }
    }

    private SaleEntity toEntity(Sale sale) {
        CustomerEntity customer = entityManager.getReference(CustomerEntity.class, sale.getCustomerId().getIdValue());
        com.minerva.infrastructure.persistence.entity.OrderEntity sourceOrder = sale.getSourceOrderId()
                .map(id -> entityManager.getReference(com.minerva.infrastructure.persistence.entity.OrderEntity.class, id.getIdValue()))
                .orElse(null);
        return new SaleEntity(sale.getId().getIdValue(), customer, sourceOrder, sale.getRegistrationDate());
    }

    private Sale.SaleItemRestoreDTO toSaleItemRestoreDTO(SaleDetailEntity entity) {
        try {
            return new Sale.SaleItemRestoreDTO(
                    new SaleDetailIdImpl(entity.getSaleDetailId()),
                    new ProductIdImpl(entity.getProduct().getProductId()),
                    new ProductQuantity(entity.getQuantity()),
                    new Money(entity.getUnitPrice())
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar un detalle de venta.", e);
        }
    }

    private Sale.PayRestoreDTO toPayRestoreDTO(PayEntity entity) {
        try {
            return new Sale.PayRestoreDTO(
                    new PayIdImpl(entity.getPayId()),
                    new Money(entity.getAmount()),
                    entity.getPaymentMethod(),
                    entity.getRegistrationDate()
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar un pago.", e);
        }
    }

    private ProductReturn toProductReturnDomain(ProductReturnEntity entity) {
        try {
            return new ProductReturn(
                    new ProductReturnIdImpl(entity.getProductReturnId()),
                    new SaleDetailIdImpl(entity.getSaleDetail().getSaleDetailId()),
                    new ProductQuantity(entity.getQuantity()), entity.getReason(), entity.getRegistrationDate()
            );
        } catch (DomainException exception) {
            throw new EntityRestoreException("Error al restaurar una devolución de producto.", exception);
        }
    }

    private SaleId createSaleId(UUID value) {
        try {
            return new SaleIdImpl(value);
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el ID de la venta.", e);
        }
    }
}
