package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.UnitToBulkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUnitToBulkRepository extends JpaRepository<UnitToBulkEntity, UnitToBulkEntity.UnitToBulkId> {

    Optional<UnitToBulkEntity> findByBulkProduct_ProductId(UUID bulkProductId);

    List<UnitToBulkEntity> findByUnitProduct_ProductId(UUID unitProductId);

    boolean existsByBulkProduct_ProductId(UUID bulkProductId);
}
