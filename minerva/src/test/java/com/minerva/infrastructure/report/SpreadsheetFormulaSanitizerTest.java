package com.minerva.infrastructure.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpreadsheetFormulaSanitizerTest {

    @Test
    void neutralizesDangerousPrefixesIncludingAfterLeadingWhitespace() {
        assertEquals("'=SUM(A1:A2)", SpreadsheetFormulaSanitizer.sanitize("=SUM(A1:A2)"));
        assertEquals("'  +cmd", SpreadsheetFormulaSanitizer.sanitize("  +cmd"));
        assertEquals("'\t-10", SpreadsheetFormulaSanitizer.sanitize("\t-10"));
        assertEquals("' @user", SpreadsheetFormulaSanitizer.sanitize(" @user"));
    }

    @Test
    void preservesSafeTextWhitespaceEmptyAndNull() {
        assertEquals("Producto", SpreadsheetFormulaSanitizer.sanitize("Producto"));
        assertEquals("  Producto", SpreadsheetFormulaSanitizer.sanitize("  Producto"));
        assertEquals("   ", SpreadsheetFormulaSanitizer.sanitize("   "));
        assertEquals("", SpreadsheetFormulaSanitizer.sanitize(""));
        assertNull(SpreadsheetFormulaSanitizer.sanitize(null));
    }
}
