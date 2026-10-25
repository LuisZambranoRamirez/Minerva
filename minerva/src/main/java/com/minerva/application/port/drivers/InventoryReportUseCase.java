package com.minerva.application.port.drivers;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;

public interface InventoryReportUseCase {
    GeneratedReport generate(InventoryReportQuery query) throws UnauthorizedActionException;
}
