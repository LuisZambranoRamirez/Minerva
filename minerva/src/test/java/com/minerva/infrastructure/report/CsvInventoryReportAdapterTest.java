package com.minerva.infrastructure.report;

import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvInventoryReportAdapterTest {

    private final CsvInventoryReportAdapter writer = new CsvInventoryReportAdapter(
            Clock.fixed(Instant.parse("2026-10-25T03:04:05Z"), ZoneId.of("America/Lima")));

    @Test
    void writesUtf8HeadersEscapedDetailAndNeutralizedFormula() throws Exception {
        var report = writer.write(List.of(row("Café, \"Premium\"\nLínea 2", "=actor")), query());
        String csv = new String(report.content(), StandardCharsets.UTF_8);

        try (CSVParser parser = CSVParser.parse(new StringReader(csv), CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get())) {
            var record = parser.getRecords().getFirst();
            assertEquals("Café, \"Premium\"\nLínea 2", record.get("Producto"));
            assertEquals("'=actor", record.get("Actor"));
            assertEquals("2.500", record.get("Cantidad"));
            assertEquals(11, record.size());
        }
        assertEquals(1, report.rowCount());
        assertEquals(ReportFormat.CSV.mediaType(), report.mediaType());
        assertEquals("movimientos-inventario-20261024-220405.csv", report.filename());
    }

    @Test
    void emptyReportContainsOnlyHeadersAndWrongFormatIsRejected() throws Exception {
        var report = writer.write(List.of(), query());
        try (CSVParser parser = CSVParser.parse(
                new String(report.content(), StandardCharsets.UTF_8), CSVFormat.DEFAULT.builder()
                        .setHeader().setSkipHeaderRecord(true).get())) {
            assertEquals(List.of(), parser.getRecords());
            assertEquals(11, parser.getHeaderMap().size());
        }
        assertThrows(IllegalArgumentException.class, () -> writer.write(List.of(),
                new InventoryReportQuery(ReportFormat.XLSX, null, null, null, null, null)));
    }

    private InventoryReportQuery query() {
        return new InventoryReportQuery(ReportFormat.CSV, null, null, null, null, null);
    }

    static InventoryReportRow row(String productName, String actor) {
        return new InventoryReportRow(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                LocalDateTime.of(2026, 10, 24, 12, 30),
                "MINV-0001-TEST",
                productName,
                ProductCategory.ABARROTES_SECOS,
                InventoryMovementType.ENTRADA,
                InventoryMovementSource.STOCK_ENTRY,
                new BigDecimal("2.500"),
                new BigDecimal("10.000"),
                new BigDecimal("12.500"),
                actor);
    }
}
