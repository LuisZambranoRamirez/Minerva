package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.InventoryMovementEntity;
import com.minerva.infrastructure.persistence.projection.InventoryReportRowProjection;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface JpaInventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID>,
        JpaSpecificationExecutor<InventoryMovementEntity> {

    boolean existsByMovementSourceAndSourceId(InventoryMovementSource source, UUID sourceId);

    @Override
    @EntityGraph(attributePaths = {"product", "actor"})
    Page<InventoryMovementEntity> findAll(Specification<InventoryMovementEntity> specification, Pageable pageable);

    @Query("""
            SELECT movement.inventoryMovementId AS movementId,
                   movement.registrationDate AS registrationDate,
                   product.sku AS sku,
                   product.productName AS productName,
                   product.category AS category,
                   movement.movementType AS type,
                   movement.movementSource AS source,
                   movement.quantity AS quantity,
                   movement.stockBefore AS stockBefore,
                   movement.stockAfter AS stockAfter,
                   actor.userName AS actorId
            FROM InventoryMovementEntity movement
            JOIN movement.product product
            JOIN movement.actor actor
            WHERE (:filterProduct = false OR product.productId = :productId)
              AND (:filterType = false OR movement.movementType = :type)
              AND (:filterSource = false OR movement.movementSource = :source)
              AND (:filterFrom = false OR movement.registrationDate >= :from)
              AND (:filterTo = false OR movement.registrationDate <= :to)
            ORDER BY movement.registrationDate ASC, movement.inventoryMovementId ASC
            """)
    List<InventoryReportRowProjection> findReportRows(
            @Param("filterProduct") boolean filterProduct,
            @Param("productId") UUID productId,
            @Param("filterType") boolean filterType,
            @Param("type") InventoryMovementType type,
            @Param("filterSource") boolean filterSource,
            @Param("source") InventoryMovementSource source,
            @Param("filterFrom") boolean filterFrom,
            @Param("from") LocalDateTime from,
            @Param("filterTo") boolean filterTo,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
