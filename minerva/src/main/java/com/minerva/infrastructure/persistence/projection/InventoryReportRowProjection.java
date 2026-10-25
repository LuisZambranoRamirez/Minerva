package com.minerva.infrastructure.persistence.projection;

import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface InventoryReportRowProjection {
    UUID getMovementId();
    LocalDateTime getRegistrationDate();
    String getSku();
    String getProductName();
    ProductCategory getCategory();
    InventoryMovementType getType();
    InventoryMovementSource getSource();
    BigDecimal getQuantity();
    BigDecimal getStockBefore();
    BigDecimal getStockAfter();
    String getActorId();
}
