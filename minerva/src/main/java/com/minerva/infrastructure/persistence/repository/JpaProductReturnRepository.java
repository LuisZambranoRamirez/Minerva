package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.ProductReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaProductReturnRepository extends JpaRepository<ProductReturnEntity, UUID> {

    List<ProductReturnEntity> findBySaleDetail_SaleDetailId(UUID saleDetailId);
}

