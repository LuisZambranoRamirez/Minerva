package com.minerva.infrastructure.persistence.repository;

import com.minerva.domain.constants.PaymentMethod;
import com.minerva.infrastructure.persistence.entity.PayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaPayRepository extends JpaRepository<PayEntity, UUID> {

    List<PayEntity> findBySaleEntity_SaleId(String saleId);

    List<PayEntity> findByPaymentMethod(PaymentMethod paymentMethod);
}
