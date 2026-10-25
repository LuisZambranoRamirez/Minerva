package com.minerva.infrastructure.report;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.application.port.driven.InventoryReportWriter;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Component
public final class ExcelInventoryReportAdapter implements InventoryReportWriter {

    private final Clock clock;

    public ExcelInventoryReportAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio.");
    }

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.XLSX;
    }

    @Override
    public GeneratedReport write(List<InventoryReportRow> rows, InventoryReportQuery query) {
        if (!supports(query.format())) {
            throw new IllegalArgumentException("El escritor Excel solo admite el formato xlsx.");
        }
        List<InventoryReportRow> safeRows = List.copyOf(rows);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = styles(workbook);
            writeDetail(workbook, safeRows, styles);
            writeSummary(workbook, safeRows, styles);
            workbook.write(output);
            return new GeneratedReport(
                    output.toByteArray(),
                    ReportFormat.XLSX.mediaType(),
                    ReportFileSupport.filename(ReportFormat.XLSX, clock),
                    safeRows.size());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el reporte Excel.", exception);
        }
    }

    private void writeDetail(Workbook workbook, List<InventoryReportRow> rows, Styles styles) {
        Sheet sheet = workbook.createSheet("Movimientos");
        Row header = sheet.createRow(0);
        for (int index = 0; index < ReportFileSupport.DETAIL_HEADERS.length; index++) {
            cell(header, index, ReportFileSupport.DETAIL_HEADERS[index], styles.header());
        }

        int rowIndex = 1;
        for (InventoryReportRow value : rows) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, value.movementId().toString());
            Cell date = row.createCell(1);
            date.setCellValue(value.registrationDate());
            date.setCellStyle(styles.date());
            text(row, 2, value.sku());
            text(row, 3, value.productName());
            text(row, 4, value.category().name());
            text(row, 5, value.type().name());
            text(row, 6, value.source().name());
            number(row, 7, value.quantity(), styles.quantity());
            number(row, 8, value.stockBefore(), styles.quantity());
            number(row, 9, value.stockAfter(), styles.quantity());
            text(row, 10, value.actorId());
        }
        sheet.createFreezePane(0, 1);
        for (int index = 0; index < ReportFileSupport.DETAIL_HEADERS.length; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private void writeSummary(Workbook workbook, List<InventoryReportRow> rows, Styles styles) {
        Sheet sheet = workbook.createSheet("Resumen");
        Row header = sheet.createRow(0);
        cell(header, 0, "Producto", styles.header());
        InventoryMovementSource[] sources = InventoryMovementSource.values();
        for (int index = 0; index < sources.length; index++) {
            cell(header, index + 1, sources[index].name(), styles.header());
        }
        cell(header, sources.length + 1, "Total", styles.header());

        Table<String, String, BigDecimal> summary = TreeBasedTable.create();
        for (InventoryReportRow row : rows) {
            String product = row.sku() + " - " + row.productName();
            BigDecimal current = summary.get(product, row.source().name());
            summary.put(product, row.source().name(),
                    (current == null ? BigDecimal.ZERO : current).add(row.quantity()));
        }

        int rowIndex = 1;
        BigDecimal[] columnTotals = new BigDecimal[sources.length];
        java.util.Arrays.fill(columnTotals, BigDecimal.ZERO);
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (String product : summary.rowKeySet()) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, product);
            BigDecimal rowTotal = BigDecimal.ZERO;
            for (int index = 0; index < sources.length; index++) {
                BigDecimal amount = summary.get(product, sources[index].name());
                amount = amount == null ? BigDecimal.ZERO : amount;
                number(row, index + 1, amount, styles.quantity());
                columnTotals[index] = columnTotals[index].add(amount);
                rowTotal = rowTotal.add(amount);
            }
            number(row, sources.length + 1, rowTotal, styles.quantity());
            grandTotal = grandTotal.add(rowTotal);
        }

        if (!rows.isEmpty()) {
            Row totals = sheet.createRow(rowIndex);
            cell(totals, 0, "TOTAL GENERAL", styles.header());
            for (int index = 0; index < sources.length; index++) {
                number(totals, index + 1, columnTotals[index], styles.total());
            }
            number(totals, sources.length + 1, grandTotal, styles.total());
        }
        sheet.createFreezePane(1, 1);
        for (int index = 0; index <= sources.length + 1; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private Styles styles(Workbook workbook) {
        CellStyle header = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle date = workbook.createCellStyle();
        date.setDataFormat(workbook.getCreationHelper().createDataFormat()
                .getFormat("yyyy-mm-dd hh:mm:ss"));
        CellStyle quantity = workbook.createCellStyle();
        quantity.setDataFormat(workbook.createDataFormat().getFormat("0.000"));
        CellStyle total = workbook.createCellStyle();
        total.cloneStyleFrom(quantity);
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        total.setFont(totalFont);
        return new Styles(header, date, quantity, total);
    }

    private void text(Row row, int index, String value) {
        row.createCell(index, CellType.STRING)
                .setCellValue(SpreadsheetFormulaSanitizer.sanitize(value));
    }

    private void number(Row row, int index, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(index, CellType.NUMERIC);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private void cell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index, CellType.STRING);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private record Styles(CellStyle header, CellStyle date, CellStyle quantity, CellStyle total) {}
}
