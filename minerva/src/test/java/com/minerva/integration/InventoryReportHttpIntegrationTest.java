package com.minerva.integration;

import com.minerva.application.model.report.ReportFormat;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryReportHttpIntegrationTest extends IntegrationTest {

    private static final String ENDPOINT = "/api/v1/products/inventory-movements/export";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousRequestIsUnauthorizedAndClienteIsForbidden() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("format", "csv"))
                .andExpect(status().isUnauthorized());

        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + api.login(customer))
                        .param("format", "csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSellerAndWarehouseCanExportThroughRealWiring() throws Exception {
        List<IntegrationFixtures.UserFixture> authorized = List.of(
                fixtures.admin(), fixtures.seller(), fixtures.warehouseUser());

        for (IntegrationFixtures.UserFixture user : authorized) {
            mockMvc.perform(get(ENDPOINT)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + api.login(user))
                            .param("format", "csv"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.parseMediaType(ReportFormat.CSV.mediaType())))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            matchesPattern("attachment; filename=\\\"movimientos-inventario-\\d{8}-\\d{6}\\.csv\\\"")));
        }

        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM audit_event
                WHERE permission = 'PRODUCT_EXPORT_INVENTORY_REPORT'
                  AND event_type = 'READ'
                  AND subject_name = 'INVENTORY_REPORTS'
                """, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(3, auditCount);
    }

    @Test
    void xlsxResponseUsesOfficialMediaTypeSafeFilenameAndValidWorkbookBytes() throws Exception {
        String jwt = api.login(fixtures.admin());

        byte[] content = mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReportFormat.XLSX.mediaType()))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("attachment; filename=\\\"movimientos-inventario-\\d{8}-\\d{6}\\.xlsx\\\"")))
                .andReturn().getResponse().getContentAsByteArray();

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            org.junit.jupiter.api.Assertions.assertEquals(List.of("Movimientos", "Resumen"),
                    java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
                            .mapToObj(workbook::getSheetName).toList());
        }
    }

    @Test
    void invalidFormatAndRangeReturn400WithoutAudit() throws Exception {
        String jwt = api.login(fixtures.admin());

        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .param("format", "pdf"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .param("format", "csv")
                        .param("from", "2026-10-25T00:00:00")
                        .param("to", "2026-10-24T00:00:00"))
                .andExpect(status().isBadRequest());

        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM audit_event
                WHERE permission = 'PRODUCT_EXPORT_INVENTORY_REPORT'
                """, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, auditCount);
    }
}
