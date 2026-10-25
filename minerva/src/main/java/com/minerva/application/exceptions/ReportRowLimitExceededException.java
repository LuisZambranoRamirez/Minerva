package com.minerva.application.exceptions;

import java.text.NumberFormat;
import java.util.Locale;

public final class ReportRowLimitExceededException extends RuntimeException {

    private static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-ES");

    public ReportRowLimitExceededException(int maximumRows) {
        super("El reporte supera el máximo de " + formatRows(maximumRows)
                + " filas. Reducí el rango de fechas o aplicá más filtros.");
    }

    private static String formatRows(int maximumRows) {
        return NumberFormat.getIntegerInstance(SPANISH_LOCALE).format(maximumRows);
    }
}
