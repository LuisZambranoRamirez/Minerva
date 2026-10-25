package com.minerva.infrastructure.report;

import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelInventoryReportAdapterTest {

    private final ExcelInventoryReportAdapter writer = new ExcelInventoryReportAdapter(
            Clock.fixed(Instant.parse("2026-10-25T03:04:05Z"), ZoneId.of("America/Lima")));

    @Test
    void writesTypedDetailAndGuavaSummaryWithMatchingTotals() throws Exception {
        InventoryReportRow first = CsvInventoryReportAdapterTest.row("=Producto", "@actor");
        InventoryReportRow second = new InventoryReportRow(
                java.util.UUID.fromString("50000000-0000-0000-0000-000000000002"),
                first.registrationDate().plusSeconds(1), first.sku(), "=Producto", first.category(),
                first.type(), InventoryMovementSource.DIRECT_SALE,
                new BigDecimal("-1.000"), new BigDecimal("12.500"),
                new BigDecimal("11.500"), first.actorId());

        var report = writer.write(List.of(first, second), query());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report.content()))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Movimientos", workbook.getSheetAt(0).getSheetName());
            assertEquals("Resumen", workbook.getSheetAt(1).getSheetName());
            var detail = workbook.getSheet("Movimientos");
            assertEquals(3, detail.getPhysicalNumberOfRows());
            assertEquals(CellType.NUMERIC, detail.getRow(1).getCell(1).getCellType());
            assertEquals(CellType.NUMERIC, detail.getRow(1).getCell(7).getCellType());
            assertEquals("'=Producto", detail.getRow(1).getCell(3).getStringCellValue());
            assertEquals("'@actor", detail.getRow(1).getCell(10).getStringCellValue());

            var summary = workbook.getSheet("Resumen");
            int totalColumn = InventoryMovementSource.values().length + 1;
            assertEquals(3, summary.getPhysicalNumberOfRows());
            assertEquals(1.5, summary.getRow(1).getCell(totalColumn).getNumericCellValue(), 0.0001);
            assertEquals(1.5, summary.getRow(2).getCell(totalColumn).getNumericCellValue(), 0.0001);
        }
        assertEquals(2, report.rowCount());
        assertEquals(ReportFormat.XLSX.mediaType(), report.mediaType());
        assertEquals("movimientos-inventario-20261024-220405.xlsx", report.filename());
    }

    @Test
    void emptyWorkbookStillContainsBothSheetsAndHeaders() throws Exception {
        var report = writer.write(List.of(), query());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report.content()))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals(1, workbook.getSheet("Movimientos").getPhysicalNumberOfRows());
            assertEquals(1, workbook.getSheet("Resumen").getPhysicalNumberOfRows());
        }
        assertThrows(IllegalArgumentException.class, () -> writer.write(List.of(),
                new InventoryReportQuery(ReportFormat.CSV, null, null, null, null, null)));
    }

    private InventoryReportQuery query() {
        return new InventoryReportQuery(ReportFormat.XLSX, null, null, null, null, null);
    }
}
