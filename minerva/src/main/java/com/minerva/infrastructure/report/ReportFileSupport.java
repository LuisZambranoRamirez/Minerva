package com.minerva.infrastructure.report;

import com.minerva.application.model.report.ReportFormat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class ReportFileSupport {

    static final String[] DETAIL_HEADERS = {
            "ID", "Fecha", "SKU", "Producto", "Categoría", "Tipo", "Origen",
            "Cantidad", "Stock anterior", "Stock resultante", "Actor"
    };
    static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    static final DateTimeFormatter TEXT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReportFileSupport() {}

    static String filename(ReportFormat format, Clock clock) {
        return "movimientos-inventario-" + FILE_TIME.format(LocalDateTime.now(clock))
                + "." + format.extension();
    }
}
