package com.minerva.application.model.report;

import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record InventoryReportRow(
        UUID movementId,
        LocalDateTime registrationDate,
        String sku,
        String productName,
        ProductCategory category,
        InventoryMovementType type,
        InventoryMovementSource source,
        BigDecimal quantity,
        BigDecimal stockBefore,
        BigDecimal stockAfter,
        String actorId
) {
    public InventoryReportRow {
        Objects.requireNonNull(movementId, "El identificador del movimiento es obligatorio.");
        Objects.requireNonNull(registrationDate, "La fecha del movimiento es obligatoria.");
        sku = requireText(sku, "El SKU es obligatorio.");
        productName = requireText(productName, "El nombre del producto es obligatorio.");
        Objects.requireNonNull(category, "La categoría es obligatoria.");
        Objects.requireNonNull(type, "El tipo de movimiento es obligatorio.");
        Objects.requireNonNull(source, "El origen del movimiento es obligatorio.");
        Objects.requireNonNull(quantity, "La cantidad es obligatoria.");
        Objects.requireNonNull(stockBefore, "El stock anterior es obligatorio.");
        Objects.requireNonNull(stockAfter, "El stock resultante es obligatorio.");
        actorId = requireText(actorId, "El actor es obligatorio.");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
