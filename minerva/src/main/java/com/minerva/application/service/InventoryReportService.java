package com.minerva.application.service;

import com.minerva.application.exceptions.ReportRowLimitExceededException;
import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.InventoryReportRepository;
import com.minerva.application.port.driven.InventoryReportWriter;
import com.minerva.application.port.driven.UserContext;
import com.minerva.application.port.drivers.InventoryReportUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import com.minerva.domain.exceptions.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class InventoryReportService implements InventoryReportUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryReportService.class);
    private static final Permission EXPORT_PERMISSION = Permission.PRODUCT_EXPORT_INVENTORY_REPORT;
    private static final int MAX_EXPORT_ROWS = 5_000;
    private static final String SPRING_ROLE_PREFIX = "ROLE_";

    private final CurrentUserProvider currentUserProvider;
    private final InventoryReportRepository repository;
    private final List<InventoryReportWriter> writers;
    private final AuditService auditService;
    private final Clock clock;

    public InventoryReportService(
            CurrentUserProvider currentUserProvider,
            InventoryReportRepository repository,
            List<InventoryReportWriter> writers,
            AuditService auditService,
            Clock clock
    ) {
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider,
                "El proveedor del usuario actual es obligatorio.");
        this.repository = Objects.requireNonNull(repository, "El repositorio del reporte es obligatorio.");
        this.writers = List.copyOf(Objects.requireNonNull(writers, "Los escritores del reporte son obligatorios."));
        this.auditService = Objects.requireNonNull(auditService, "El servicio de auditoría es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio.");
    }

    @Override
    public GeneratedReport generate(InventoryReportQuery query) throws UnauthorizedActionException {
        Objects.requireNonNull(query, "La consulta del reporte es obligatoria.");

        UUID exportId = UUID.randomUUID();
        long startedAt = clock.millis();
        UserContext actor = currentUserProvider.currentUser();
        int rowCount = -1;

        logAttempt(exportId, actor.userId(), query, rowCount, startedAt, "STARTED");
        try {
            requirePermission(actor);
            InventoryReportWriter writer = selectWriter(query);
            List<InventoryReportRow> rows = repository.findForReport(
                    query, InventoryReportRepository.MAX_QUERY_ROWS);
            rowCount = rows.size();

            if (rowCount > MAX_EXPORT_ROWS) {
                throw new ReportRowLimitExceededException(MAX_EXPORT_ROWS);
            }

            GeneratedReport report = writer.write(List.copyOf(rows), query);
            registerSuccessfulExport(actor.userId(), exportId, query, rowCount);
            logAttempt(exportId, actor.userId(), query, rowCount, startedAt, "SUCCESS");
            return report;
        } catch (RuntimeException exception) {
            logAttempt(exportId, actor.userId(), query, rowCount, startedAt, "FAILED");
            throw exception;
        }
    }

    private void requirePermission(UserContext actor) {
        String roleName = actor.role();
        if (roleName == null || roleName.isBlank()) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para exportar reportes de inventario.");
        }
        String normalized = roleName.startsWith(SPRING_ROLE_PREFIX)
                ? roleName.substring(SPRING_ROLE_PREFIX.length())
                : roleName;
        try {
            if (Role.valueOf(normalized).lacksPermission(EXPORT_PERMISSION)) {
                throw new UnauthorizedActionException(
                        "El usuario no tiene permiso para exportar reportes de inventario.");
            }
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedActionException(
                    "El usuario no tiene permiso para exportar reportes de inventario.");
        }
    }

    private InventoryReportWriter selectWriter(InventoryReportQuery query) {
        return writers.stream()
                .filter(writer -> writer.supports(query.format()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe un escritor configurado para el formato " + query.format().extension() + "."));
    }

    private void registerSuccessfulExport(
            String actorId,
            UUID exportId,
            InventoryReportQuery query,
            int rowCount
    ) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("exportId", exportId.toString());
        metadata.put("format", query.format().extension());
        metadata.put("rowCount", Integer.toString(rowCount));
        putIfPresent(metadata, "productId", query.productId());
        putIfPresent(metadata, "type", query.type());
        putIfPresent(metadata, "source", query.source());
        putIfPresent(metadata, "from", query.from());
        putIfPresent(metadata, "to", query.to());
        metadata.put("outcome", "SUCCESS");

        try {
            auditService.register(new AuditEvent(
                    actorId,
                    EXPORT_PERMISSION,
                    new CollectionAuditTarget(CollectionAuditTarget.Resource.INVENTORY_REPORTS, metadata)
            ));
        } catch (DomainException exception) {
            throw new IllegalStateException("No se pudo registrar la auditoría del reporte.", exception);
        }
    }

    private static void putIfPresent(Map<String, String> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value.toString());
        }
    }

    private void logAttempt(
            UUID exportId,
            String actorId,
            InventoryReportQuery query,
            int rowCount,
            long startedAt,
            String outcome
    ) {
        long durationMs = Math.max(0, clock.millis() - startedAt);
        String rows = rowCount < 0 ? "unknown" : Integer.toString(rowCount);
        if ("FAILED".equals(outcome)) {
            LOGGER.error("inventory_report exportId={} actor={} format={} rows={} durationMs={} outcome={}",
                    exportId, actorId, query.format().extension(), rows, durationMs, outcome);
        } else {
            LOGGER.info("inventory_report exportId={} actor={} format={} rows={} durationMs={} outcome={}",
                    exportId, actorId, query.format().extension(), rows, durationMs, outcome);
        }
    }
}
