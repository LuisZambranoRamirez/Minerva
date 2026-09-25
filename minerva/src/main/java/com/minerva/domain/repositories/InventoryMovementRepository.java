package com.minerva.domain.repositories;

import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import com.minerva.domain.entities.product.ProductId;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryMovementRepository {
    void append(InventoryMovement movement);
    void appendAll(List<InventoryMovement> movements);
    List<InventoryMovement> findAll(ProductId productId, InventoryMovementType type,
                                    InventoryMovementSource source, LocalDateTime from, LocalDateTime to,
                                    int page, int size);
}
