package com.minerva.infrastructure.rest.controller;

import com.minerva.application.exceptions.ReportRowLimitExceededException;
import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.application.port.drivers.InventoryReportUseCase;
import com.minerva.application.port.drivers.ProductUseCase;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import com.minerva.infrastructure.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryReportControllerTest {

    private InventoryReportUseCase reportUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reportUseCase = mock(InventoryReportUseCase.class);
        ProductController controller = new ProductController(mock(ProductUseCase.class), reportUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exportsCsvWithMappedFiltersAndDownloadHeaders() throws Exception {
        byte[] bytes = "csv-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(reportUseCase.generate(any())).thenReturn(new GeneratedReport(
                bytes, ReportFormat.CSV.mediaType(),
                "movimientos-inventario-20261024-220405.csv", 1));
        UUID productId = UUID.fromString("20000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/v1/products/inventory-movements/export")
                        .param("format", "CSV")
                        .param("productId", productId.toString())
                        .param("type", "entrada")
                        .param("source", "stock_entry")
                        .param("from", "2026-10-24T08:00:00")
                        .param("to", "2026-10-24T18:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType(ReportFormat.CSV.mediaType())))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"movimientos-inventario-20261024-220405.csv\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().bytes(bytes));

        ArgumentCaptor<InventoryReportQuery> query = ArgumentCaptor.forClass(InventoryReportQuery.class);
        verify(reportUseCase).generate(query.capture());
        assertEquals(ReportFormat.CSV, query.getValue().format());
        assertEquals(productId, query.getValue().productId());
        assertEquals(InventoryMovementType.ENTRADA, query.getValue().type());
        assertEquals(InventoryMovementSource.STOCK_ENTRY, query.getValue().source());
        assertEquals(LocalDateTime.parse("2026-10-24T08:00:00"), query.getValue().from());
        assertEquals(LocalDateTime.parse("2026-10-24T18:00:00"), query.getValue().to());
    }

    @Test
    void exportsXlsxWithOfficialMediaTypeAndContractualFilename() throws Exception {
        byte[] bytes = {1, 2, 3};
        when(reportUseCase.generate(any())).thenReturn(new GeneratedReport(
                bytes, ReportFormat.XLSX.mediaType(),
                "movimientos-inventario-20261024-220405.xlsx", 0));

        mockMvc.perform(get("/api/v1/products/inventory-movements/export").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReportFormat.XLSX.mediaType()))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"movimientos-inventario-20261024-220405.xlsx\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void missingOrMalformedParametersReturnBadRequestWithoutCallingUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/products/inventory-movements/export"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/inventory-movements/export").param("format", "pdf"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/inventory-movements/export")
                        .param("format", "csv").param("productId", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/inventory-movements/export")
                        .param("format", "csv").param("type", "unknown"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/inventory-movements/export")
                        .param("format", "csv").param("source", "unknown"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/inventory-movements/export")
                        .param("format", "csv").param("from", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportUseCase);
    }

    @Test
    void invertedRangeReturnsBadRequestBeforeCallingUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/products/inventory-movements/export")
                        .param("format", "csv")
                        .param("from", "2026-10-25T00:00:00")
                        .param("to", "2026-10-24T00:00:00"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportUseCase);
    }

    @Test
    void rowLimitMapsTo422WithoutBinaryBody() throws Exception {
        when(reportUseCase.generate(any())).thenThrow(new ReportRowLimitExceededException(5_000));

        mockMvc.perform(get("/api/v1/products/inventory-movements/export").param("format", "csv"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("5.000")));
    }

    @Test
    void writerFailureMapsToControlled500WithoutPartialFile() throws Exception {
        when(reportUseCase.generate(any())).thenThrow(new IllegalStateException("writer failed"));

        mockMvc.perform(get("/api/v1/products/inventory-movements/export").param("format", "xlsx"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intentá nuevamente más tarde."));
    }

    @Test
    void unsafeFilenameFromWriterIsRejectedAsInternalError() throws Exception {
        when(reportUseCase.generate(any())).thenReturn(new GeneratedReport(
                new byte[]{1}, ReportFormat.CSV.mediaType(), "../../report.csv", 0));

        mockMvc.perform(get("/api/v1/products/inventory-movements/export").param("format", "csv"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
