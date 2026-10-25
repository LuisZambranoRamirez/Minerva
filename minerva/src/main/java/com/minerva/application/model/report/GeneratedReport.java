package com.minerva.application.model.report;

import java.util.Objects;

public record GeneratedReport(byte[] content, String mediaType, String filename, int rowCount) {
    public GeneratedReport {
        content = Objects.requireNonNull(content, "El contenido del reporte es obligatorio.").clone();
        mediaType = requireText(mediaType, "El tipo de contenido es obligatorio.");
        filename = requireText(filename, "El nombre del archivo es obligatorio.");
        if (rowCount < 0) {
            throw new IllegalArgumentException("La cantidad de filas no puede ser negativa.");
        }
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
