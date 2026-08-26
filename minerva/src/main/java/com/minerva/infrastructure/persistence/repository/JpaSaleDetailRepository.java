package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.SaleDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSaleDetailRepository extends JpaRepository<SaleDetailEntity, UUID> {

    List<SaleDetailEntity> findBySaleEntity_SaleId(String saleId);

    List<SaleDetailEntity> findByProductEntity_ProductNameId(String productNameId);
}
