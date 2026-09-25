package com.minerva.infrastructure.rest.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CustomerSelfServiceSecurityConfigStaticTest {

    @Test
    void securitySeparatesPublicRegistrationClienteSelfServiceAdminApprovalAndEmployeeApis() throws IOException {
        String security = Files.readString(Path.of("src/main/java/com/minerva/infrastructure/rest/config/SecurityConfig.java"));

        assertTrue(security.contains("/api/v1/auth/customer-register"));
        assertTrue(security.contains(".permitAll()"));
        assertTrue(security.contains("/api/v1/admin/customer-accounts/**"));
        assertTrue(security.contains("hasRole(\"ADMIN\")"));
        assertTrue(security.contains("/api/v1/customer/**"));
        assertTrue(security.contains("hasRole(\"CLIENTE\")"));
        assertTrue(security.contains("/api/v1/users/**") && security.contains("/api/v1/orders/**"));
        assertFalse(security.contains("hasAnyRole(\"ADMIN\", \"VENDEDOR\", \"ALMACENISTA\", \"CLIENTE\")"),
                "CLIENTE must not be added to employee/inventory/order/sale endpoint matchers");
    }
}