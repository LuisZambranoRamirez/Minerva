package com.minerva.infrastructure.persistence.repository;

import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.infrastructure.persistence.entity.InventoryLossEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaInventoryLossRepository extends JpaRepository<InventoryLossEntity, UUID> {

    List<InventoryLossEntity> findByProduct_ProductId(UUID productId);

    List<InventoryLossEntity> findByReason(InventoryLossReason reason);
}
