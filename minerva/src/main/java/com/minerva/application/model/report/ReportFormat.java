package com.minerva.application.model.report;

import java.util.Locale;

public enum ReportFormat {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv; charset=UTF-8");

    private final String extension;
    private final String mediaType;

    ReportFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public static ReportFormat from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El formato del reporte es obligatorio.");
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El formato debe ser xlsx o csv.", exception);
        }
    }

    public String extension() {
        return extension;
    }

    public String mediaType() {
        return mediaType;
    }
}
