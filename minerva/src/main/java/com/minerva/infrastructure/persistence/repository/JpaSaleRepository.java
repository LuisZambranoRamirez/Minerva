package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface JpaSaleRepository extends JpaRepository<SaleEntity, UUID> {

    List<SaleEntity> findByCustomer_CustomerId(UUID customerId);
    Optional<SaleEntity> findBySourceOrder_OrderId(UUID orderId);
}
