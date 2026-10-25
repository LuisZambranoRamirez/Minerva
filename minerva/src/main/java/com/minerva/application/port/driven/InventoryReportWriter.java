package com.minerva.application.port.driven;

import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;

import java.util.List;

public interface InventoryReportWriter {
    boolean supports(ReportFormat format);

    GeneratedReport write(List<InventoryReportRow> rows, InventoryReportQuery query);
}
