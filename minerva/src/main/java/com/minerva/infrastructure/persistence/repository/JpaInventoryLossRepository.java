package com.minerva.infrastructure.persistence.repository;

import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.infrastructure.persistence.entity.InventoryLossEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaInventoryLossRepository extends JpaRepository<InventoryLossEntity, UUID> {

    List<InventoryLossEntity> findByProductEntity_ProductNameId(String productNameId);

    List<InventoryLossEntity> findByReason(InventoryLossReason reason);
}
