package com.minerva.application.port.driven;

import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;

import java.util.List;

public interface InventoryReportRepository {

    int MAX_QUERY_ROWS = 5_001;

    List<InventoryReportRow> findForReport(InventoryReportQuery query, int limit)
            throws IllegalArgumentException;
}
