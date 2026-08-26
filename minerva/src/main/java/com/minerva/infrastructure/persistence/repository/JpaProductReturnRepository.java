package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.ProductReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaProductReturnRepository extends JpaRepository<ProductReturnEntity, UUID> {

    List<ProductReturnEntity> findBySaleDetailEntity_SaleDetailId(String saleDetailId);
}

