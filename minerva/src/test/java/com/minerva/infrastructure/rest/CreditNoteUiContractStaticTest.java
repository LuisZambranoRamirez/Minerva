package com.minerva.infrastructure.rest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CreditNoteUiContractStaticTest {

    @Test
    void salesPageRendersEmbeddedNotesAndUsesParallelTopLevelRequests() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/pages/employee/sales.js"));

        assertThat(script).contains("Promise.all([", "sale.creditNotes ?? []", "error.status === 409",
                "sale.creditStatus", "ACREDITADA_TOTALMENTE");
        assertThat(script).doesNotContain("/credit-notes`, {token:");
    }

    @Test
    void dashboardConsumesTheBackendSummaryWithoutPageAggregation() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/pages/employee/dashboard.js"));

        assertThat(script).contains("/api/v1/sales/summary", "salesSummary.creditedTotal",
                "salesSummary.amountDue", "salesSummary.creditBalance");
        assertThat(script).doesNotContain("reduce((sum, sale)");
    }
}
