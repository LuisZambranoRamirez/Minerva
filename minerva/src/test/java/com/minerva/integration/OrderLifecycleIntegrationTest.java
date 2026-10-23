package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderLifecycleIntegrationTest extends IntegrationTest {

    @Test
    void customerCanListReadAndCancelOnlyOwnOrderWithPersistedReason() throws Exception {
        IntegrationFixtures.UserFixture firstCustomer = fixtures.approvedCustomerUser();
        IntegrationFixtures.UserFixture secondCustomer = fixtures.secondApprovedCustomerUser();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        String firstJwt = api.login(firstCustomer);
        String secondJwt = api.login(secondCustomer);
        String firstOrder = checkout(firstJwt, product.id().toString(), "1.000");
        String secondOrder = checkout(secondJwt, product.id().toString(), "1.000");

        api.get("/api/v1/customer/orders", firstJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(firstOrder));
        api.get("/api/v1/customer/orders/" + firstOrder, firstJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(firstOrder));
        api.get("/api/v1/customer/orders/" + secondOrder, firstJwt)
                .andExpect(status().isNotFound());
        api.post("/api/v1/customer/orders/" + secondOrder + "/cancel", firstJwt,
                        Map.of("reason", "Intento de cancelar un pedido ajeno"))
                .andExpect(status().isNotFound());
        assertEquals("PENDIENTE", jdbcTemplate.queryForObject(
                "SELECT status::text FROM customer_order WHERE order_id = ?::uuid",
                String.class,
                secondOrder
        ));

        api.post("/api/v1/customer/orders/" + firstOrder + "/cancel", firstJwt,
                        Map.of("reason", "Ya no necesito el pedido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        api.get("/api/v1/customer/orders/" + firstOrder + "/transitions", firstJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].previousStatus").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].newStatus").value("CANCELADO"))
                .andExpect(jsonPath("$[1].actorId").value(firstCustomer.username()))
                .andExpect(jsonPath("$[1].cancellationReason").value("Ya no necesito el pedido"));

        assertEquals("Ya no necesito el pedido", jdbcTemplate.queryForObject("""
                SELECT cancellation_reason FROM order_transition
                WHERE id_order = ?::uuid AND new_status = 'CANCELADO'
                """, String.class, firstOrder));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement"));
        assertDecimal("10.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
    }

    @Test
    void employeeTransitionsDecrementOnceRestoreOnceAndDeliveryCreatesExactlyOneSale() throws Exception {
        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.UserFixture warehouse = fixtures.warehouseUser();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        String customerJwt = api.login(customer);
        String sellerJwt = api.login(seller);
        String warehouseJwt = api.login(warehouse);

        String cancellableOrder = checkout(customerJwt, product.id().toString(), "2.000");
        api.post("/api/v1/orders/" + cancellableOrder + "/confirm", sellerJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
        assertDecimal("8.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount(cancellableOrder, "ORDER_CONFIRMATION"));

        api.post("/api/v1/orders/" + cancellableOrder + "/confirm", sellerJwt)
                .andExpect(status().isConflict());
        assertDecimal("8.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount(cancellableOrder, "ORDER_CONFIRMATION"));

        api.post("/api/v1/orders/" + cancellableOrder + "/cancel", sellerJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        assertDecimal("10.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount(cancellableOrder, "ORDER_CANCELLATION"));

        api.post("/api/v1/orders/" + cancellableOrder + "/cancel", sellerJwt)
                .andExpect(status().isConflict());
        assertDecimal("10.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount(cancellableOrder, "ORDER_CANCELLATION"));

        String deliveredOrder = checkout(customerJwt, product.id().toString(), "1.000");
        api.post("/api/v1/orders/" + deliveredOrder + "/confirm", sellerJwt).andExpect(status().isOk());
        api.post("/api/v1/orders/" + deliveredOrder + "/prepare", warehouseJwt).andExpect(status().isOk());
        api.post("/api/v1/orders/" + deliveredOrder + "/dispatch", warehouseJwt).andExpect(status().isOk());
        api.post("/api/v1/orders/" + deliveredOrder + "/deliver", warehouseJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGADO"));
        api.post("/api/v1/orders/" + deliveredOrder + "/deliver", warehouseJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGADO"));

        assertEquals(1, count("SELECT COUNT(*) FROM sale WHERE source_order_id = '" + deliveredOrder + "'::uuid"));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM sale_detail d
                JOIN sale s ON s.sale_id = d.id_sale
                WHERE s.source_order_id = '%s'::uuid
                """.formatted(deliveredOrder)));
        assertDecimal("9.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
    }

    private String checkout(String jwt, String productId, String quantity) throws Exception {
        String response = api.post("/api/v1/customer/orders", jwt, Map.of(
                        "items", List.of(Map.of(
                                "productId", productId,
                                "quantity", new BigDecimal(quantity)
                        ))
                ))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.text(response, "orderId");
    }

    private int movementCount(String orderId, String source) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_movement
                WHERE source_id = ?::uuid AND movement_source = CAST(? AS inventory_movement_source)
                """, Integer.class, orderId, source);
        return count == null ? 0 : count;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private BigDecimal decimalValue(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, arguments);
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
