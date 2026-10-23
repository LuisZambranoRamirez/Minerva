package com.minerva.integration;

import com.minerva.integration.support.ConcurrentTestExecutor;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditNoteIntegrationTest extends IntegrationTest {

    @Test
    void issueReloadListAndSummaryExposeTheSamePartialThenFullCredit() throws Exception {
        Scenario scenario = saleWithTwoProducts();
        UUID detail = jdbcTemplate.queryForObject(
                "SELECT sale_detail_id FROM sale_detail ORDER BY sale_detail_id LIMIT 1", UUID.class);

        api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), Map.of(
                        "reference", "NC-PARTIAL",
                        "lines", List.of(line(detail, "0.500", "DAÑADO"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(6.00))
                .andExpect(jsonPath("$.issuedBy").value("vendedor01"))
                .andExpect(jsonPath("$.lines[0].reason").value("DAÑADO"));

        api.get("/api/v1/sales/" + scenario.saleId(), scenario.jwt())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCredited").value(6.00))
                .andExpect(jsonPath("$.amountDue").value(18.00))
                .andExpect(jsonPath("$.creditStatus").value("ACREDITADA_PARCIALMENTE"))
                .andExpect(jsonPath("$.creditNotes[0].reference").value("NC-PARTIAL"));

        api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), Map.of(
                        "reference", "NC-REMAINDER",
                        "lines", List.of(line(detail, "0.500", "DAÑADO"))))
                .andExpect(status().isCreated());

        api.get("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saleId").value(scenario.saleId().toString()))
                .andExpect(jsonPath("$[0].issuedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].issuedBy").value("vendedor01"))
                .andExpect(jsonPath("$[0].lines[0].saleDetailId").value(detail.toString()))
                .andExpect(jsonPath("$[0].lines[0].reason").value("DAÑADO"))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        api.get("/api/v1/sales/" + scenario.saleId(), scenario.jwt())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCredited").value(12.00))
                .andExpect(jsonPath("$.amountDue").value(12.00))
                .andExpect(jsonPath("$.status").value("ACREDITADA_PARCIAL"))
                .andExpect(jsonPath("$.creditStatus").value("ACREDITADA_PARCIALMENTE"));
        api.get("/api/v1/sales", scenario.jwt())
                .andExpect(jsonPath("$[0].totalCredited").value(12.00))
                .andExpect(jsonPath("$[0].creditStatus").value("ACREDITADA_PARCIALMENTE"));
        api.get("/api/v1/sales/summary", scenario.jwt())
                .andExpect(jsonPath("$.creditedTotal").value(12.00))
                .andExpect(jsonPath("$.amountDue").value(12.00));
    }

    @Test
    void aFullyCreditedSaleUsesAdditiveCreditStatusWithoutChangingLegacyStatus() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        IntegrationFixtures.SaleFixture sale = fixtures.sale(customer, product);
        String jwt = api.login(seller);

        api.post("/api/v1/sales/" + sale.id() + "/credit-notes", jwt, Map.of(
                        "lines", List.of(line(sale.detailId(), "1.000", "DAÑADO"))))
                .andExpect(status().isCreated());
        api.get("/api/v1/sales/" + sale.id(), jwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue").value(0.00))
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.creditStatus").value("ACREDITADA_TOTALMENTE"));
    }

    @Test
    void getCreditNotesRequiresAuthenticationAndReadPermissionWithoutMutatingHistory() throws Exception {
        Scenario scenario = saleWithTwoProducts();
        int before = count("SELECT COUNT(*) FROM credit_note");

        api.get("/api/v1/sales/" + scenario.saleId() + "/credit-notes")
                .andExpect(status().isUnauthorized());
        api.get("/api/v1/sales/" + scenario.saleId() + "/credit-notes", api.login(fixtures.warehouseUser()))
                .andExpect(status().isForbidden());

        assertEquals(before, count("SELECT COUNT(*) FROM credit_note"));
    }

    @Test
    void legacyReturnEndpointKeeps201AndCreatesOneAuditableCreditLine() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        IntegrationFixtures.SaleFixture sale = fixtures.sale(customer, product);
        String jwt = api.login(seller);

        api.post("/api/v1/sales/details/" + sale.detailId() + "/returns", jwt,
                        Map.of("quantity", new BigDecimal("1.000"), "reason", "EQUIVOCACION"))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));

        api.get("/api/v1/sales/" + sale.id() + "/credit-notes", jwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].issuedBy").value("vendedor01"))
                .andExpect(jsonPath("$[0].total").value(12.00))
                .andExpect(jsonPath("$[0].lines[0].quantity").value(1.000))
                .andExpect(jsonPath("$[0].lines[0].reason").value("EQUIVOCACION"));
    }

    @Test
    void previousSaleConsumerCanKeepReadingTheOriginalProjectionAndIgnoreAdditiveFields() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        IntegrationFixtures.SaleFixture sale = fixtures.sale(customer, product);
        String response = api.get("/api/v1/sales/" + sale.id(), api.login(seller))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var legacyProjection = json.read(response);
        assertEquals(sale.id().toString(), legacyProjection.get("saleId").stringValue());
        assertEquals(0, new BigDecimal("12.00").compareTo(legacyProjection.get("total").decimalValue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(legacyProjection.get("totalPaid").decimalValue()));
        assertEquals(0, new BigDecimal("12.00").compareTo(legacyProjection.get("amountDue").decimalValue()));
        assertEquals("PENDIENTE", legacyProjection.get("status").stringValue());
    }

    @Test
    void postgresqlEnforcesCreditNoteReferenceAndSaleForeignKeyConstraints() {
        UUID missingSale = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO credit_note (credit_note_id, reference, sale_id, issued_at, issued_by)
                VALUES (?, 'NC-ORPHAN', ?, CURRENT_TIMESTAMP, NULL)
                """, UUID.randomUUID(), missingSale));

        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("1.000"));
        IntegrationFixtures.SaleFixture sale = fixtures.sale(customer, product);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO credit_note (credit_note_id, reference, sale_id, issued_at, issued_by)
                VALUES (?, '   ', ?, CURRENT_TIMESTAMP, NULL)
                """, UUID.randomUUID(), sale.id()));
    }

    @Test
    void invalidSecondLineRollsBackTheWholeMultiLineCreditAndInventory() throws Exception {
        Scenario scenario = saleWithTwoProducts();
        List<UUID> details = jdbcTemplate.queryForList(
                "SELECT sale_detail_id FROM sale_detail ORDER BY sale_detail_id", UUID.class);

        api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), Map.of(
                        "reference", "NC-ATOMIC",
                        "lines", List.of(
                                line(details.get(0), "1.000", "EQUIVOCACION"),
                                line(details.get(1), "99.000", "DAÑADO"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CREDIT_NOTE_QUANTITY_EXCEEDED"));

        assertEquals(0, count("SELECT COUNT(*) FROM credit_note"));
        assertEquals(0, count("SELECT COUNT(*) FROM product_return"));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement WHERE movement_source = 'PRODUCT_RETURN'"));
    }

    @Test
    void creditEndpointsEnforceAuthenticationAuthorizationAndConflictContract() throws Exception {
        Scenario scenario = saleWithTwoProducts();
        UUID detail = jdbcTemplate.queryForObject("SELECT sale_detail_id FROM sale_detail LIMIT 1", UUID.class);
        Map<String, Object> body = Map.of("lines", List.of(line(detail, "2.000", "DAÑADO")));

        api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", body)
                .andExpect(status().isUnauthorized());
        String warehouseJwt = api.login(fixtures.warehouseUser());
        api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", warehouseJwt, body)
                .andExpect(status().isForbidden());
        api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void concurrentCreditsSerializeAndOnlyOneConsumesTheRemainingQuantity() throws Exception {
        Scenario scenario = saleWithTwoProducts();
        UUID detail = jdbcTemplate.queryForObject("SELECT sale_detail_id FROM sale_detail LIMIT 1", UUID.class);
        Map<String, Object> body = Map.of("lines", List.of(line(detail, "1.000", "DAÑADO")));

        List<ConcurrentTestExecutor.Outcome<Integer>> outcomes = concurrent.runPair(
                () -> api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), body)
                        .andReturn().getResponse().getStatus(),
                () -> api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), body)
                        .andReturn().getResponse().getStatus());

        assertEquals(1, outcomes.stream().filter(ConcurrentTestExecutor.Outcome::succeeded)
                .map(ConcurrentTestExecutor.Outcome::value).filter(value -> value == 201).count());
        assertEquals(1, count("SELECT COUNT(*) FROM credit_note"));
        assertEquals(1, count("SELECT COUNT(*) FROM product_return"));
    }

    @Test
    void concurrentPaymentAndCreditShareTheSaleLockAndNeverProduceNegativeDebt() throws Exception {
        Scenario scenario = saleWithTwoProducts();
        UUID detail = jdbcTemplate.queryForObject("SELECT sale_detail_id FROM sale_detail LIMIT 1", UUID.class);

        List<ConcurrentTestExecutor.Outcome<Integer>> outcomes = concurrent.runPair(
                () -> api.post("/api/v1/sales/" + scenario.saleId() + "/payments", scenario.jwt(), Map.of(
                                "payments", List.of(Map.of("amount", new BigDecimal("12.00"), "paymentMethod", "EFECTIVO"))))
                        .andReturn().getResponse().getStatus(),
                () -> api.post("/api/v1/sales/" + scenario.saleId() + "/credit-notes", scenario.jwt(), Map.of(
                                "lines", List.of(line(detail, "1.000", "DAÑADO"))))
                        .andReturn().getResponse().getStatus());

        assertTrue(outcomes.stream().allMatch(ConcurrentTestExecutor.Outcome::succeeded));
        api.get("/api/v1/sales/" + scenario.saleId(), scenario.jwt())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue").isNumber());
        BigDecimal applied = decimalValue("SELECT COALESCE(SUM(amount), 0) FROM pay")
                .add(decimalValue("""
                        SELECT COALESCE(SUM(pr.quantity * sd.unit_price), 0)
                        FROM product_return pr JOIN sale_detail sd ON sd.sale_detail_id = pr.id_sale_detail
                        """));
        assertTrue(applied.compareTo(new BigDecimal("24.00")) <= 0);
    }

    private Scenario saleWithTwoProducts() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture first = fixtures.product(new BigDecimal("10.000"));
        IntegrationFixtures.ProductFixture second = fixtures.secondProduct(new BigDecimal("10.000"));
        String jwt = api.login(seller);
        api.post("/api/v1/sales", jwt, Map.of("customerId", customer.id().toString(), "payments", List.of(),
                        "items", List.of(item(first.id()), item(second.id()))))
                .andExpect(status().isCreated());
        return new Scenario(jwt, jdbcTemplate.queryForObject("SELECT sale_id FROM sale", UUID.class));
    }

    private Map<String, Object> item(UUID productId) {
        return Map.of("productId", productId.toString(), "quantity", new BigDecimal("1.000"));
    }

    private Map<String, Object> line(UUID detailId, String quantity, String reason) {
        return Map.of("saleDetailId", detailId.toString(), "quantity", new BigDecimal(quantity), "reason", reason);
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private BigDecimal decimalValue(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }

    private record Scenario(String jwt, UUID saleId) {}
}
