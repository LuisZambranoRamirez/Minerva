package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.SaleDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSaleDetailRepository extends JpaRepository<SaleDetailEntity, UUID> {

    List<SaleDetailEntity> findBySale_SaleId(UUID saleId);

    List<SaleDetailEntity> findByProduct_ProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select detail from SaleDetailEntity detail join fetch detail.product where detail.saleDetailId = :id")
    Optional<SaleDetailEntity> findByIdForProductReturn(@Param("id") UUID id);
}
