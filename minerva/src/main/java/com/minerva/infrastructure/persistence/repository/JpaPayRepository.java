package com.minerva.infrastructure.persistence.repository;

import com.minerva.domain.constants.PaymentMethod;
import com.minerva.infrastructure.persistence.entity.PayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

public interface JpaPayRepository extends JpaRepository<PayEntity, UUID> {

    List<PayEntity> findBySale_SaleId(UUID saleId);
    List<PayEntity> findBySale_SaleIdIn(Collection<UUID> saleIds);

    List<PayEntity> findByPaymentMethod(PaymentMethod paymentMethod);
}
