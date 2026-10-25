package com.minerva.infrastructure.report;

import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.application.port.driven.InventoryReportWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Component
public final class CsvInventoryReportAdapter implements InventoryReportWriter {

    private final Clock clock;

    public CsvInventoryReportAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio.");
    }

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.CSV;
    }

    @Override
    public GeneratedReport write(List<InventoryReportRow> rows, InventoryReportQuery query) {
        if (!supports(query.format())) {
            throw new IllegalArgumentException("El escritor CSV solo admite el formato csv.");
        }
        List<InventoryReportRow> safeRows = List.copyOf(rows);

        try (StringWriter output = new StringWriter();
             CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.builder()
                     .setHeader(ReportFileSupport.DETAIL_HEADERS)
                     .get())) {
            for (InventoryReportRow row : safeRows) {
                printer.printRecord(
                        text(row.movementId().toString()),
                        ReportFileSupport.TEXT_TIME.format(row.registrationDate()),
                        text(row.sku()),
                        text(row.productName()),
                        text(row.category().name()),
                        text(row.type().name()),
                        text(row.source().name()),
                        row.quantity().toPlainString(),
                        row.stockBefore().toPlainString(),
                        row.stockAfter().toPlainString(),
                        text(row.actorId()));
            }
            printer.flush();
            return new GeneratedReport(
                    output.toString().getBytes(StandardCharsets.UTF_8),
                    ReportFormat.CSV.mediaType(),
                    ReportFileSupport.filename(ReportFormat.CSV, clock),
                    safeRows.size());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el reporte CSV.", exception);
        }
    }

    private String text(String value) {
        return SpreadsheetFormulaSanitizer.sanitize(value);
    }
}
