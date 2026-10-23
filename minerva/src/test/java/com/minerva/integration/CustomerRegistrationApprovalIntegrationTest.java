package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerRegistrationApprovalIntegrationTest extends IntegrationTest {

    private static final String REGISTER = "/api/v1/auth/customer-register";

    @Test
    void publicRegistrationPersistsPendingAccountRejectsLoginAndAdminApprovalEnablesAccess() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        Map<String, Object> request = registration("clientepublico", "20600000101", "10000101",
                "900001001", "cliente.publico@minerva.test");

        api.post(REGISTER, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("clientepublico"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.customerId").isString());

        assertEquals("PENDING_APPROVAL", stringValue(
                "SELECT approval_status::text FROM app_user WHERE user_name = ?", "clientepublico"));
        assertEquals(1, count("SELECT COUNT(*) FROM customer WHERE ruc = '20600000101'"));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM audit_event
                WHERE user_name = 'clientepublico' AND permission = 'CUSTOMER_REGISTER'
                """));

        api.loginAttempt("clientepublico", IntegrationFixtures.PASSWORD)
                .andExpect(status().isUnauthorized());

        String adminJwt = api.login(admin);
        api.post("/api/v1/admin/customer-accounts/clientepublico/approve", adminJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(admin.username()))
                .andExpect(jsonPath("$.approvedDate").isNotEmpty())
                .andExpect(jsonPath("$.rejectedDate").doesNotExist())
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        Map<String, Object> metadata = jdbcTemplate.queryForMap("""
                SELECT approval_status::text AS status, approved_date, approved_by,
                       rejected_date, rejected_by, rejection_reason
                FROM app_user WHERE user_name = ?
                """, "clientepublico");
        assertEquals("APPROVED", metadata.get("status"));
        assertNotNull(metadata.get("approved_date"));
        assertEquals(admin.username(), metadata.get("approved_by"));
        assertNull(metadata.get("rejected_date"));
        assertNull(metadata.get("rejected_by"));
        assertNull(metadata.get("rejection_reason"));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM audit_event
                WHERE user_name = 'admin01' AND permission = 'USER_REGISTER'
                  AND subject_id = 'clientepublico'
                """));

        String customerJwt = api.login("clientepublico", IntegrationFixtures.PASSWORD);
        api.get("/api/v1/customer/catalog", customerJwt)
                .andExpect(status().isOk());
    }

    @Test
    void duplicateRegistrationReturnsConflictWithoutPartialCustomerUserOrAudit() throws Exception {
        Map<String, Object> request = registration("clienteduplicado", "20600000102", "10000102",
                "900001002", "cliente.duplicado@minerva.test");

        api.post(REGISTER, request).andExpect(status().isCreated());
        api.post(REGISTER, request).andExpect(status().isConflict());

        assertEquals(1, count("SELECT COUNT(*) FROM customer WHERE full_name <> 'anonimo'"));
        assertEquals(1, count("SELECT COUNT(*) FROM personal"));
        assertEquals(1, count("SELECT COUNT(*) FROM app_user"));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM audit_event WHERE permission = 'CUSTOMER_REGISTER'
                """));
    }

    @Test
    void invalidPublicRegistrationIsRejectedBeforePersistence() throws Exception {
        Map<String, Object> request = registration("clienteinvalido", "RUC-invalido", "10000103",
                "900001003", "cliente.invalido@minerva.test");

        api.post(REGISTER, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.ruc").exists());

        assertEquals(0, count("SELECT COUNT(*) FROM personal"));
        assertEquals(0, count("SELECT COUNT(*) FROM app_user"));
        assertEquals(0, count("SELECT COUNT(*) FROM customer WHERE full_name <> 'anonimo'"));
    }

    @Test
    void adminRejectionPersistsReasonActorDateAndAuditAndKeepsLoginDisabled() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        api.post(REGISTER, registration("clienterechazado", "20600000104", "10000104",
                "900001004", "cliente.rechazado@minerva.test"))
                .andExpect(status().isCreated());
        String adminJwt = api.login(admin);

        api.post("/api/v1/admin/customer-accounts/clienterechazado/reject", adminJwt,
                        Map.of("reason", "Documentación inconsistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedBy").value(admin.username()))
                .andExpect(jsonPath("$.rejectedDate").isNotEmpty())
                .andExpect(jsonPath("$.rejectionReason").value("Documentación inconsistente"))
                .andExpect(jsonPath("$.approvedDate").doesNotExist());

        Map<String, Object> metadata = jdbcTemplate.queryForMap("""
                SELECT approval_status::text AS status, approved_date, approved_by,
                       rejected_date, rejected_by, rejection_reason
                FROM app_user WHERE user_name = ?
                """, "clienterechazado");
        assertEquals("REJECTED", metadata.get("status"));
        assertNull(metadata.get("approved_date"));
        assertNull(metadata.get("approved_by"));
        assertNotNull(metadata.get("rejected_date"));
        assertEquals(admin.username(), metadata.get("rejected_by"));
        assertEquals("Documentación inconsistente", metadata.get("rejection_reason"));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM audit_event
                WHERE user_name = 'admin01' AND permission = 'USER_REGISTER'
                  AND subject_id = 'clienterechazado'
                """));

        api.loginAttempt("clienterechazado", IntegrationFixtures.PASSWORD)
                .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> registration(
            String username,
            String ruc,
            String dni,
            String phone,
            String email
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("businessName", "Negocio " + username);
        request.put("legalName", "Empresa " + username + " SAC");
        request.put("ruc", ruc);
        request.put("address", "Av. Registro 100");
        request.put("defaultDeliveryAddress", "Av. Entrega 200");
        request.put("defaultDeliveryContact", "Contacto Registro");
        request.put("defaultDeliveryPhone", "922222224");
        request.put("contactDni", dni);
        request.put("contactNames", "Cliente");
        request.put("contactLastNames", username);
        request.put("contactPhone", phone);
        request.put("contactEmail", email);
        request.put("username", username);
        request.put("password", IntegrationFixtures.PASSWORD);
        return request;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String stringValue(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, String.class, arguments);
    }
}
