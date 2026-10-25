package com.minerva.integration;

import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.application.port.driven.InventoryReportRepository;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class InventoryReportPersistenceIntegrationTest extends IntegrationTest {

    private static final LocalDateTime FIRST_TIME = IntegrationFixtures.FIXED_TIME.plusHours(1);
    private static final LocalDateTime SECOND_TIME = FIRST_TIME.plusHours(1);

    @Autowired
    private InventoryReportRepository reports;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void combinedFiltersAreInclusiveAndEmptyResultIsSupported() {
        var actor = fixtures.admin();
        var firstProduct = fixtures.product(new BigDecimal("10.000"));
        var secondProduct = fixtures.secondProduct(new BigDecimal("20.000"));
        insertMovement(uuid(2), firstProduct.id(), "2.000", "10.000", "12.000",
                "ENTRADA", "STOCK_ENTRY", actor.username(), FIRST_TIME);
        insertMovement(uuid(3), firstProduct.id(), "-1.000", "12.000", "11.000",
                "SALIDA", "DIRECT_SALE", actor.username(), SECOND_TIME);
        insertMovement(uuid(4), secondProduct.id(), "3.000", "20.000", "23.000",
                "ENTRADA", "STOCK_ENTRY", actor.username(), FIRST_TIME);

        List<InventoryReportRow> matching = reports.findForReport(new InventoryReportQuery(
                ReportFormat.CSV,
                firstProduct.id(),
                InventoryMovementType.ENTRADA,
                InventoryMovementSource.STOCK_ENTRY,
                FIRST_TIME,
                FIRST_TIME
        ), InventoryReportRepository.MAX_QUERY_ROWS);

        assertEquals(1, matching.size());
        assertEquals(uuid(2), matching.getFirst().movementId());
        assertEquals("MINV-0001-TEST", matching.getFirst().sku());
        assertEquals("Producto Integración", matching.getFirst().productName());
        assertEquals(actor.username(), matching.getFirst().actorId());

        List<InventoryReportRow> empty = reports.findForReport(new InventoryReportQuery(
                ReportFormat.XLSX, firstProduct.id(), InventoryMovementType.ENTRADA,
                InventoryMovementSource.DIRECT_SALE, FIRST_TIME, SECOND_TIME
        ), InventoryReportRepository.MAX_QUERY_ROWS);

        assertEquals(List.of(), empty);
    }

    @Test
    void rowsAreOrderedByDateThenMovementIdAndLoadedWithOneQuery() {
        var actor = fixtures.admin();
        var product = fixtures.product(new BigDecimal("10.000"));
        insertMovement(uuid(9), product.id(), "1.000", "10.000", "11.000",
                "ENTRADA", "STOCK_ENTRY", actor.username(), SECOND_TIME);
        insertMovement(uuid(3), product.id(), "1.000", "11.000", "12.000",
                "ENTRADA", "STOCK_ENTRY", actor.username(), FIRST_TIME);
        insertMovement(uuid(2), product.id(), "1.000", "12.000", "13.000",
                "ENTRADA", "STOCK_ENTRY", actor.username(), FIRST_TIME);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<InventoryReportRow> rows = reports.findForReport(new InventoryReportQuery(
                ReportFormat.CSV, null, null, null, null, null
        ), InventoryReportRepository.MAX_QUERY_ROWS);

        assertEquals(List.of(uuid(2), uuid(3), uuid(9)),
                rows.stream().map(InventoryReportRow::movementId).toList());
        assertEquals(1L, statistics.getPrepareStatementCount(),
                "La proyección JOIN debe resolver producto y actor sin lookups por fila ni COUNT adicional.");
    }

    @Test
    void queryReturnsAtMostFiveThousandAndOneRowsWithoutCount() {
        var actor = fixtures.admin();
        var product = fixtures.product(new BigDecimal("6000.000"));
        jdbcTemplate.update("""
                INSERT INTO inventory_movement (
                    inventory_movement_id, id_product, quantity, stock_before, stock_after,
                    movement_type, movement_source, source_id, actor_user_name, registration_date
                )
                SELECT md5(series::text)::uuid, ?, 1.000, 6000.000, 6001.000,
                       CAST('ENTRADA' AS inventory_movement_type),
                       CAST('STOCK_ENTRY' AS inventory_movement_source),
                       NULL, ?, ? + (series * INTERVAL '1 millisecond')
                FROM generate_series(1, 5002) AS series
                """, product.id(), actor.username(), FIRST_TIME);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<InventoryReportRow> rows = reports.findForReport(new InventoryReportQuery(
                ReportFormat.XLSX, null, null, null, null, null
        ), InventoryReportRepository.MAX_QUERY_ROWS);

        assertEquals(5_001, rows.size());
        assertEquals(1L, statistics.getPrepareStatementCount(),
                "La detección máximo+1 no debe ejecutar COUNT.");
        assertThrows(IllegalArgumentException.class, () -> reports.findForReport(
                new InventoryReportQuery(ReportFormat.CSV, null, null, null, null, null), 5_002));
    }

    private void insertMovement(
            UUID movementId,
            UUID productId,
            String quantity,
            String stockBefore,
            String stockAfter,
            String type,
            String source,
            String actor,
            LocalDateTime registrationDate
    ) {
        jdbcTemplate.update("""
                INSERT INTO inventory_movement (
                    inventory_movement_id, id_product, quantity, stock_before, stock_after,
                    movement_type, movement_source, source_id, actor_user_name, registration_date
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS inventory_movement_type),
                          CAST(? AS inventory_movement_source), NULL, ?, ?)
                """,
                movementId, productId, new BigDecimal(quantity), new BigDecimal(stockBefore),
                new BigDecimal(stockAfter), type, source, actor, registrationDate);
    }

    private static UUID uuid(int suffix) {
        return UUID.fromString("50000000-0000-0000-0000-" + String.format("%012d", suffix));
    }
}
