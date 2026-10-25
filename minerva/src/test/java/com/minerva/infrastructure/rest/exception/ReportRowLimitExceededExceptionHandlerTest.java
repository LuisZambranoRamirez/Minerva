package com.minerva.infrastructure.rest.exception;

import com.minerva.application.exceptions.ReportRowLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportRowLimitExceededExceptionHandlerTest {

    @Test
    void rowLimitExceededMapsTo422WithoutBinaryResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/v1/products/inventory-movements/export");

        var response = handler.handleReportRowLimitExceeded(
                new ReportRowLimitExceededException(5_000), request);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().status());
        assertEquals("/api/v1/products/inventory-movements/export", response.getBody().path());
        assertTrue(response.getBody().message().contains("5.000"),
                "El error debe informar el límite para que el usuario reduzca el rango o aplique filtros.");
    }
}
