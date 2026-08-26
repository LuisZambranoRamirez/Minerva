package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSaleRepository extends JpaRepository<SaleEntity, UUID> {

    List<SaleEntity> findByCustomerEntity_CustomerNameId(String customerNameId);
    Optional<SaleEntity> findById(String id);
}
