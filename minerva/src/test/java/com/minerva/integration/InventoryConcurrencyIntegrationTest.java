package com.minerva.integration;

import com.minerva.integration.support.ConcurrentTestExecutor;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryConcurrencyIntegrationTest extends IntegrationTest {

    @Test
    void competingOrderConfirmationsUseIndependentTransactionsAndNeverOversell() throws Exception {
        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("2.000"));
        String customerJwt = api.login(customer);
        String sellerJwt = api.login(seller);
        String firstOrder = checkout(customerJwt, product.id().toString());
        String secondOrder = checkout(customerJwt, product.id().toString());

        List<ConcurrentTestExecutor.Outcome<Integer>> outcomes = concurrent.runPair(
                () -> api.post("/api/v1/orders/" + firstOrder + "/confirm", sellerJwt)
                        .andReturn().getResponse().getStatus(),
                () -> api.post("/api/v1/orders/" + secondOrder + "/confirm", sellerJwt)
                        .andReturn().getResponse().getStatus()
        );

        assertEquals(2, outcomes.size());
        assertTrue(successfulResponses(outcomes) <= 1);

        concurrent.readCommitted(() -> {
            assertEquals(1, count("SELECT COUNT(*) FROM customer_order WHERE status = 'CONFIRMADO'"));
            assertEquals(1, count("SELECT COUNT(*) FROM customer_order WHERE status = 'PENDIENTE'"));
            BigDecimal finalStock = decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id());
            assertEquals(0, new BigDecimal("0.000").compareTo(finalStock));
            assertTrue(finalStock.signum() >= 0);
            assertEquals(1, count("""
                    SELECT COUNT(*) FROM inventory_movement
                    WHERE movement_source = 'ORDER_CONFIRMATION'
                    """));
            assertEquals(1, count("""
                    SELECT COUNT(DISTINCT source_id) FROM inventory_movement
                    WHERE movement_source = 'ORDER_CONFIRMATION'
                    """));
            return null;
        });
    }

    private String checkout(String jwt, String productId) throws Exception {
        String response = api.post("/api/v1/customer/orders", jwt, Map.of(
                        "items", List.of(Map.of(
                                "productId", productId,
                                "quantity", new BigDecimal("2.000")
                        ))
                ))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.text(response, "orderId");
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
}
