package com.minerva.infrastructure.report;

public final class SpreadsheetFormulaSanitizer {

    private SpreadsheetFormulaSanitizer() {}

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) return value;

        int firstSignificant = 0;
        while (firstSignificant < value.length()
                && Character.isWhitespace(value.charAt(firstSignificant))) {
            firstSignificant++;
        }
        if (firstSignificant == value.length()) return value;

        char first = value.charAt(firstSignificant);
        return first == '=' || first == '+' || first == '-' || first == '@'
                ? "'" + value
                : value;
    }
}
