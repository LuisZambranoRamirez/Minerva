package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.OrderTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface JpaOrderTransitionRepository extends JpaRepository<OrderTransitionEntity, UUID> {
    List<OrderTransitionEntity> findByOrder_OrderIdOrderByRegistrationDateAsc(UUID orderId);
    List<OrderTransitionEntity> findByOrder_OrderIdAndOrder_Customer_CustomerIdOrderByRegistrationDateAsc(UUID orderId, UUID customerId);
}
