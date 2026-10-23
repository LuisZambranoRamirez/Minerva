package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface JpaSaleRepository extends JpaRepository<SaleEntity, UUID> {

    List<SaleEntity> findByCustomer_CustomerId(UUID customerId);
    Optional<SaleEntity> findBySourceOrder_OrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaleEntity s where s.saleId = :id")
    Optional<SaleEntity> findByIdForUpdate(@Param("id") UUID id);
}
