package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.orderId = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<OrderEntity> findByOrderIdAndCustomer_CustomerId(UUID orderId, UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.orderId = :id and o.customer.customerId = :customerId")
    Optional<OrderEntity> findByIdAndCustomerIdForUpdate(@Param("id") UUID id, @Param("customerId") UUID customerId);

    List<OrderEntity> findByCustomer_CustomerIdOrderByRegistrationDateDesc(UUID customerId);

}
