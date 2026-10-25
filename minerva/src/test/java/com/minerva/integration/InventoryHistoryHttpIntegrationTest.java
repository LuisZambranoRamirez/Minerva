package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryHistoryHttpIntegrationTest extends IntegrationTest {

    private static final String ENDPOINT = "/api/v1/products/inventory-movements";
    private static final LocalDateTime TIME = IntegrationFixtures.FIXED_TIME.plusHours(1);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void globalAndLegacyEndpointsFilterInDatabaseWithStablePagination() throws Exception {
        var actor = fixtures.admin();
        var product = fixtures.product(new BigDecimal("20.000"));
        insertMovement(uuid(3), product.id(), "ENTRADA", "STOCK_ENTRY", actor.username(), TIME);
        insertMovement(uuid(2), product.id(), "ENTRADA", "STOCK_ENTRY", actor.username(), TIME);
        insertMovement(uuid(9), product.id(), "SALIDA", "DIRECT_SALE", actor.username(), TIME.plusHours(1));
        String token = api.login(actor);

        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("productId", product.id().toString())
                        .param("type", "entrada")
                        .param("source", "stock_entry")
                        .param("from", TIME.toString())
                        .param("to", TIME.toString())
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inventoryMovementId").value(uuid(2).toString()))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/v1/products/{productId}/movements", product.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("type", "ENTRADA")
                        .param("source", "STOCK_ENTRY")
                        .param("from", TIME.toString())
                        .param("to", TIME.toString())
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inventoryMovementId").value(uuid(3).toString()));
    }

    @Test
    void historyAndCsvExportUseTheSameFiltersAndOrder() throws Exception {
        var actor = fixtures.admin();
        var product = fixtures.product(new BigDecimal("20.000"));
        insertMovement(uuid(3), product.id(), "ENTRADA", "STOCK_ENTRY", actor.username(), TIME);
        insertMovement(uuid(2), product.id(), "ENTRADA", "STOCK_ENTRY", actor.username(), TIME);
        String token = api.login(actor);

        String history = mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("productId", product.id().toString())
                        .param("type", "ENTRADA").param("source", "STOCK_ENTRY")
                        .param("from", TIME.toString()).param("to", TIME.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String csv = mockMvc.perform(get(ENDPOINT + "/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("format", "csv")
                        .param("productId", product.id().toString())
                        .param("type", "ENTRADA").param("source", "STOCK_ENTRY")
                        .param("from", TIME.toString()).param("to", TIME.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> historyIds = java.util.stream.IntStream.range(0, json.read(history).size())
                .mapToObj(index -> json.read(history).get(index).get("inventoryMovementId").stringValue())
                .toList();
        assertTrue(csv.indexOf(historyIds.get(0)) < csv.indexOf(historyIds.get(1)));
    }

    @Test
    void readSecurityAllowsStaffAndRejectsAnonymousAndCustomer() throws Exception {
        mockMvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        var customer = fixtures.approvedCustomerUser();
        mockMvc.perform(get(ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + api.login(customer)))
                .andExpect(status().isForbidden());

        for (var user : List.of(fixtures.admin(), fixtures.seller(), fixtures.warehouseUser())) {
            mockMvc.perform(get(ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + api.login(user)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void invalidFiltersAndPaginationReturn400() throws Exception {
        String token = api.login(fixtures.admin());
        for (String query : List.of(
                "productId=not-a-uuid", "type=unknown", "source=unknown", "from=not-a-date",
                "from=2026-10-25T10:00:00&to=2026-10-24T10:00:00", "page=-1", "size=0", "size=101")) {
            mockMvc.perform(get(ENDPOINT + "?" + query)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    private void insertMovement(UUID id, UUID productId, String type, String source,
                                String actor, LocalDateTime time) {
        boolean entry = "ENTRADA".equals(type);
        jdbcTemplate.update("""
                INSERT INTO inventory_movement (
                    inventory_movement_id, id_product, quantity, stock_before, stock_after,
                    movement_type, movement_source, source_id, actor_user_name, registration_date
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS inventory_movement_type),
                          CAST(? AS inventory_movement_source), NULL, ?, ?)
                """, id, productId, new BigDecimal(entry ? "1.000" : "-1.000"),
                new BigDecimal("10.000"), new BigDecimal(entry ? "11.000" : "9.000"),
                type, source, actor, time);
    }

    private static UUID uuid(int suffix) {
        return UUID.fromString("51000000-0000-0000-0000-" + String.format("%012d", suffix));
    }
}
