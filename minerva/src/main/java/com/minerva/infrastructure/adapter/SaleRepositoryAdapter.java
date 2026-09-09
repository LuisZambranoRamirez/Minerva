package com.minerva.infrastructure.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.sale.PayId;
import com.minerva.domain.entities.sale.SaleDetailId;
import com.minerva.domain.entities.sale.SaleId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.minerva.domain.entities.sale.Sale.PayDTO;
import com.minerva.domain.entities.sale.Sale.SaleDetailDTO;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.id.SaleIdImpl;
import com.minerva.domain.repositories.SaleRepository;
import com.minerva.infrastructure.persistence.entity.CustomerEntity;
import com.minerva.infrastructure.persistence.entity.PayEntity;
import com.minerva.infrastructure.persistence.entity.ProductEntity;
import com.minerva.infrastructure.persistence.entity.SaleDetailEntity;
import com.minerva.infrastructure.persistence.entity.SaleEntity;
import com.minerva.infrastructure.persistence.repository.JpaPayRepository;
import com.minerva.infrastructure.persistence.repository.JpaSaleDetailRepository;
import com.minerva.infrastructure.persistence.repository.JpaSaleRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class SaleRepositoryAdapter implements SaleRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final JpaSaleRepository saleRepository;
    private final JpaPayRepository payRepository;
    private final JpaSaleDetailRepository saleDetailRepository;

    public SaleRepositoryAdapter(JpaSaleRepository jpaSaleRepository, JpaPayRepository jpaPayRepository, JpaSaleDetailRepository jpaSaleDetailRepository) {
        this.saleRepository = jpaSaleRepository;
        this.payRepository = jpaPayRepository;
        this.saleDetailRepository = jpaSaleDetailRepository;
    }

    @Transactional
    @Override
    public void save(com.minerva.domain.entities.sale.Sale sale) {
        saleRepository.save(toEntity(sale));
        saveSaleDetails(sale.getSaleDetails(), sale.getId());
        savePays(sale.getPays(), sale.getId());
    }

    @Transactional
    public void saveSaleDetails(List<SaleDetailDTO> saleDetails, SaleId saleId) {
        SaleEntity sale = entityManager.getReference(SaleEntity.class, saleId.value());

        for (SaleDetailDTO saleDetailDTO : saleDetails) {
            ProductEntity product = entityManager.getReference(ProductEntity.class, saleDetailDTO.productId());
            
            SaleDetailEntity saleDetail = new SaleDetailEntity(saleDetailDTO.saleDetailId(), sale, product, saleDetailDTO.quantity(), saleDetailDTO.unitPrice());
            saleDetailRepository.save(saleDetail);
        }        
    }

    @Transactional
    public void savePays(List<PayDTO> pays, SaleId saleId) {
        SaleEntity sale = entityManager.getReference(SaleEntity.class, saleId.value());

        for (PayDTO payDTO : pays) {
            PayEntity pay = new PayEntity(payDTO.payId(), sale, payDTO.amount(), payDTO.paymentMethod(), payDTO.registrationDate());
            payRepository.save(pay);
        }        
    }

    public List<SaleDetailDTO> findSaleDetailBySaleId(SaleId saleId) {
        return saleDetailRepository.findBySaleEntity_SaleId(saleId.getIdValueAsString())
                .stream()
                .map(this::toSaleDetailDTO)
                .toList();
    }

    public List<PayDTO> findPayBySaleId(SaleId saleId) {
        return payRepository.findBySaleEntity_SaleId(saleId.getIdValueAsString())
                .stream()
                .map(this::toPayDTO)
                .toList();
    }

    @Override
    public Optional<com.minerva.domain.entities.sale.Sale> findById(SaleId saleId) {
        Optional<SaleEntity> saleEntity = saleRepository.findById(saleId.getIdValueAsString());
        if (saleEntity.isEmpty()) return Optional.empty();

        List<SaleDetailDTO> saleDetails = findSaleDetailBySaleId(saleId);
        List<PayDTO> pays = findPayBySaleId(saleId);

        return Optional.of(toDomain(saleEntity.get(), saleDetails, pays));
    }

    @Override
    public List<com.minerva.domain.entities.sale.Sale> findByCustomerId(CustomerId customerId) {
        List<SaleEntity> saleEntities = saleRepository.findByCustomerEntity_CustomerNameId(customerId.value());

        return saleEntities.stream()
        .map(sale -> {
            SaleIdImpl saleIdImpl;

            // OJAZOOO, esto hay que revisar porque no creo que el domain expecion deberia manejarse aqui y/o asi
            // aparte, tengo dudas sobre si deberia lanzar UnexpectedDomainException
            try {
                saleIdImpl = SaleIdImpl.fromString(sale.getSaleId());
            } catch (DomainException e) {
                throw new UnexpectedDomainException("Error al convertir el ID de venta: " + e.getMessage(), e);
            }

            List<SaleDetailDTO> saleDetails =
                    findSaleDetailBySaleId(saleIdImpl);

            List<PayDTO> pays =
                    findPayBySaleId(saleIdImpl);

            return toDomain(sale, saleDetails, pays);
        })
        .toList();
    }

    @Override
    public List<com.minerva.domain.entities.sale.Sale> findAll() {
        List<com.minerva.domain.entities.sale.Sale> sales;

        List<SaleEntity> saleEntities = saleRepository.findAll();
        List<SaleDetailEntity> saleDetails = saleDetailRepository.findAll();
        List<PayEntity> pays = payRepository.findAll();

        sales = new ArrayList<>(saleEntities.size());


        Map<String, List<SaleDetailDTO>> detailsBySaleId = saleDetails.stream().collect(Collectors.groupingBy(
                sd -> sd.getSale().getSaleId(),
                Collectors.mapping(this::toSaleDetailDTO, Collectors.toList())
            ));

        Map<String, List<PayDTO>> paysBySaleId = pays.stream().collect(Collectors.groupingBy(
                p -> p.getSale().getSaleId(),
                Collectors.mapping(this::toPayDTO, Collectors.toList())
            ));

        for (SaleEntity sale : saleEntities) {
            List<SaleDetailDTO> detailDTOs = detailsBySaleId.getOrDefault(sale.getSaleId(), List.of());

            List<PayDTO> payDTOs = paysBySaleId.getOrDefault(sale.getSaleId(), List.of());

            sales.add(toDomain(sale, detailDTOs, payDTOs));
        }

        return sales;
    }


    @Override
    public List<SaleDetailDTO> findSaleDetailsById(SaleDetailId id) {
        return saleDetailRepository.findById(id.getIdValueAsString())
                .stream()
                .map(this::toSaleDetailDTO)
                .toList();
    }

    @Override
    public List<PayDTO> findPaysById(PayId id) {
        return payRepository.findById(id.getIdValueAsString())
                .stream()
                .map(this::toPayDTO)
                .toList();
    }    
 

    private com.minerva.domain.entities.sale.Sale toDomain(SaleEntity entity, List<SaleDetailDTO> saleDetailDTO, List<PayDTO> payDTO) {
        return new com.minerva.domain.entities.sale.Sale(
                entity.getSaleId(),
                entity.getCustomerEntity().getCustomerNameId(),
                entity.getRegistrationDate(),
                saleDetailDTO,
                payDTO
        );
    }

    private SaleEntity toEntity(com.minerva.domain.entities.sale.Sale sale) {
        CustomerEntity customerEntity = entityManager
        .getReference(CustomerEntity.class, sale.getCustomerId());

        return new SaleEntity(
                sale.getId().getIdValueAsString(),
                customerEntity,
                sale.getRegistrationDate()
        );
    }

    private SaleDetailDTO toSaleDetailDTO(SaleDetailEntity entity) {
        return new SaleDetailDTO(
                entity.getSaleDetailId(),
                entity.getProduct().getProductNameId(),
                entity.getQuantity(),
                entity.getUnitPrice()
        );
    }

    private PayDTO toPayDTO(PayEntity entity) {
        return new PayDTO(
                entity.getPayId(),
                entity.getAmount(),
                entity.getPaymentMethod(),
                entity.getRegistrationDate()
        );
    }

}
