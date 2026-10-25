package com.minerva.application.model.report;

import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReportQuery(
        ReportFormat format,
        UUID productId,
        InventoryMovementType type,
        InventoryMovementSource source,
        LocalDateTime from,
        LocalDateTime to
) {
    public InventoryReportQuery {
        if (format == null) {
            throw new IllegalArgumentException("El formato del reporte es obligatorio.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    public InventoryMovementFilter filter() {
        return new InventoryMovementFilter(productId, type, source, from, to);
    }
}
