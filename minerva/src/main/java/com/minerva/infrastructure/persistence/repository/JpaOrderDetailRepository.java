package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.OrderDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface JpaOrderDetailRepository extends JpaRepository<OrderDetailEntity, UUID> {
    List<OrderDetailEntity> findByOrder_OrderIdOrderByProduct_ProductIdAsc(UUID orderId);
}
