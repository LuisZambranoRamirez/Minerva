package com.minerva.application.model.report;

import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryReportContractsTest {

    @Test
    void reportFormatAcceptsOnlyCanonicalFormatsIgnoringCase() {
        assertEquals(ReportFormat.XLSX, ReportFormat.from("XLSX"));
        assertEquals(ReportFormat.CSV, ReportFormat.from("csv"));
        assertThrows(IllegalArgumentException.class, () -> ReportFormat.from(null));
        assertThrows(IllegalArgumentException.class, () -> ReportFormat.from("pdf"));
        assertThrows(IllegalArgumentException.class, () -> ReportFormat.from(" csv "));
    }

    @Test
    void queryAcceptsOptionalFiltersAndInclusiveSingleInstantRange() {
        LocalDateTime instant = LocalDateTime.of(2026, 10, 24, 12, 0);

        InventoryReportQuery query = new InventoryReportQuery(
                ReportFormat.CSV, null, null, null, instant, instant);

        assertEquals(ReportFormat.CSV, query.format());
        assertNull(query.productId());
        assertEquals(instant, query.from());
        assertEquals(instant, query.to());
    }

    @Test
    void queryRejectsMissingFormatAndInvertedRange() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 25, 0, 0);
        LocalDateTime to = from.minusSeconds(1);

        assertThrows(IllegalArgumentException.class,
                () -> new InventoryReportQuery(null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryReportQuery(ReportFormat.XLSX, null, null, null, from, to));
    }

    @Test
    void reportRowRequiresCompleteTypedDetail() {
        InventoryReportRow row = validRow();

        assertEquals("ABCD-1234-EFGH", row.sku());
        assertEquals(new BigDecimal("2.500"), row.quantity());
        assertThrows(NullPointerException.class, () -> new InventoryReportRow(
                null, row.registrationDate(), row.sku(), row.productName(), row.category(), row.type(),
                row.source(), row.quantity(), row.stockBefore(), row.stockAfter(), row.actorId()));
        assertThrows(IllegalArgumentException.class, () -> new InventoryReportRow(
                row.movementId(), row.registrationDate(), " ", row.productName(), row.category(), row.type(),
                row.source(), row.quantity(), row.stockBefore(), row.stockAfter(), row.actorId()));
    }

    @Test
    void generatedReportIsImmutableAndValidatesMetadata() {
        byte[] source = {1, 2, 3};
        GeneratedReport report = new GeneratedReport(source, ReportFormat.CSV.mediaType(), "reporte.csv", 0);
        source[0] = 9;
        byte[] exposed = report.content();
        exposed[1] = 9;

        assertArrayEquals(new byte[]{1, 2, 3}, report.content());
        assertEquals(0, report.rowCount());
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedReport(new byte[0], "text/csv", "reporte.csv", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedReport(new byte[0], " ", "reporte.csv", 0));
    }

    private static InventoryReportRow validRow() {
        return new InventoryReportRow(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 10, 24, 11, 30),
                "ABCD-1234-EFGH",
                "Arroz premium",
                ProductCategory.ABARROTES_SECOS,
                InventoryMovementType.ENTRADA,
                InventoryMovementSource.STOCK_ENTRY,
                new BigDecimal("2.500"),
                new BigDecimal("10.000"),
                new BigDecimal("12.500"),
                "admin"
        );
    }
}
