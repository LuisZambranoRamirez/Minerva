package com.minerva.infrastructure.adapter;

import java.util.List;
import java.util.Optional;

import com.minerva.domain.entities.supplier.SupplierId;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.valueObject.RUC;
import com.minerva.domain.valueObject.SupplierName;
import com.minerva.domain.repositories.SupplierRepository;
import com.minerva.infrastructure.persistence.entity.SupplierEntity;
import com.minerva.infrastructure.persistence.repository.JpaSupplierRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierRepositoryAdapter implements SupplierRepository{

    private final JpaSupplierRepository jpaSupplierRepository;

    public SupplierRepositoryAdapter(JpaSupplierRepository jpaSupplierRepository) {
        this.jpaSupplierRepository = jpaSupplierRepository;
    }

    @Override
    public void save(com.minerva.domain.entities.supplier.Supplier supplier) {
        jpaSupplierRepository.save(toEntity(supplier));
    }

    @Override
    public boolean existsById(SupplierId id) {
        return jpaSupplierRepository.existsById(id.getIdValue());
    }

    @Override
    public boolean existsBySupplierName(SupplierName supplierName) {
        return jpaSupplierRepository.existsBySupplierName(supplierName.getValue());
    }

    @Override
    public boolean existsByRuc(RUC ruc) {
        return jpaSupplierRepository.existsByRuc(ruc.getValue());
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        return jpaSupplierRepository.existsByPhoneNumber(phoneNumber.getValue());
    }

    @Override
    public List<com.minerva.domain.entities.supplier.Supplier> findAll() {
        return jpaSupplierRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<com.minerva.domain.entities.supplier.Supplier> findById(SupplierId id) {
        return jpaSupplierRepository.findById(id.getIdValue())
                .map(this::toDomain);
    }
    

    @Override
    public Optional<com.minerva.domain.entities.supplier.Supplier> findByRuc(RUC ruc) {
        return jpaSupplierRepository.findByRuc(ruc.getValue())
                .map(this::toDomain);
    }

    @Override
    public Optional<com.minerva.domain.entities.supplier.Supplier> findByPhone(PhoneNumber phoneNumber) {
        return jpaSupplierRepository.findByPhoneNumber(phoneNumber.getValue())
                .map(this::toDomain);
    }

    private SupplierEntity toEntity(com.minerva.domain.entities.supplier.Supplier supplier) {
        return new SupplierEntity(
                supplier.getId().getIdValue(),
                supplier.getSupplierName().getValue(),
                supplier.getRuc().map(RUC::getValue).orElse(null),
                supplier.getPhoneNumber().map(PhoneNumber::getValue).orElse(null),
                supplier.getRegistrationDate()
        );
    }

    private com.minerva.domain.entities.supplier.Supplier toDomain(SupplierEntity entity) {
        return new com.minerva.domain.entities.supplier.Supplier(
                entity.getSupplierId(),
                entity.getSupplierName(),
                entity.getRuc(),
                entity.getPhoneNumber(),
                entity.getRegistrationDate()
        );
    }
}
