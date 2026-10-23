package com.minerva.integration;

import com.minerva.integration.support.ConcurrentTestExecutor;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReturnConcurrencyIntegrationTest extends IntegrationTest {

    @Test
    void concurrentDuplicateReturnCompensatesInventoryAtMostOnceRegardlessOfWorkerOutcome() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("5.000"));
        String jwt = api.login(seller);
        api.post("/api/v1/sales", jwt, Map.of(
                        "customerId", customer.id().toString(),
                        "items", List.of(Map.of(
                                "productId", product.id().toString(),
                                "quantity", new BigDecimal("1.000")
                        )),
                        "payments", List.of()
                ))
                .andExpect(status().isCreated());
        UUID saleDetailId = jdbcTemplate.queryForObject("SELECT sale_detail_id FROM sale_detail", UUID.class);
        Map<String, Object> body = Map.of(
                "quantity", new BigDecimal("1.000"),
                "reason", "EQUIVOCACION"
        );

        List<ConcurrentTestExecutor.Outcome<Integer>> outcomes = concurrent.runPair(
                () -> api.post("/api/v1/sales/details/" + saleDetailId + "/returns", jwt, body)
                        .andReturn().getResponse().getStatus(),
                () -> api.post("/api/v1/sales/details/" + saleDetailId + "/returns", jwt, body)
                        .andReturn().getResponse().getStatus()
        );

        assertEquals(2, outcomes.size());
        assertTrue(successfulResponses(outcomes) <= 1);

        concurrent.readCommitted(() -> {
            assertEquals(1, count("SELECT COUNT(*) FROM product_return"));
            assertDecimal("1.000", decimalValue("SELECT SUM(quantity) FROM product_return"));
            assertDecimal("5.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
            assertEquals(1, count("""
                    SELECT COUNT(*) FROM inventory_movement
                    WHERE movement_source = 'PRODUCT_RETURN'
                    """));
            Map<String, Object> movement = jdbcTemplate.queryForMap("""
                    SELECT quantity, stock_before, stock_after FROM inventory_movement
                    WHERE movement_source = 'PRODUCT_RETURN'
                    """);
            assertDecimal("1.000", (BigDecimal) movement.get("quantity"));
            assertDecimal("4.000", (BigDecimal) movement.get("stock_before"));
            assertDecimal("5.000", (BigDecimal) movement.get("stock_after"));
            return null;
        });
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private long successfulResponses(List<ConcurrentTestExecutor.Outcome<Integer>> outcomes) {
        return outcomes.stream()
                .filter(ConcurrentTestExecutor.Outcome::succeeded)
                .map(ConcurrentTestExecutor.Outcome::value)
                .filter(status -> status >= 200 && status < 300)
                .count();
    }

    private BigDecimal decimalValue(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, arguments);
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
