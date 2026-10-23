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

class OrderCheckoutIntegrationTest extends IntegrationTest {

    @Test
    void checkoutUsesAuthenticatedCustomerAuthoritativePriceAndDefaultDeliveryData() throws Exception {
        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        String jwt = api.login(customer);

        String response = api.post("/api/v1/customer/orders", jwt, Map.of(
                        "items", List.of(Map.of(
                                "productId", product.id().toString(),
                                "quantity", new BigDecimal("2.000")
                        )),
                        "deliveryAddress", "   ",
                        "deliveryContact", "",
                        "deliveryPhone", " "
                ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.details[0].unitPrice").value(12.0))
                .andExpect(jsonPath("$.details[0].subtotal").value(24.0))
                .andExpect(jsonPath("$.total").value(24.0))
                .andExpect(jsonPath("$.deliveryAddress").value("Av. Entregas 200"))
                .andExpect(jsonPath("$.deliveryContact").value("Contacto Test"))
                .andExpect(jsonPath("$.deliveryPhone").value("922222222"))
                .andReturn().getResponse().getContentAsString();

        String orderId = json.text(response, "orderId");
        Map<String, Object> persisted = jdbcTemplate.queryForMap("""
                SELECT o.id_customer, o.status::text AS status, d.quantity, d.unit_price,
                       o.delivery_address, o.delivery_contact, o.delivery_phone
                FROM customer_order o
                JOIN order_detail d ON d.id_order = o.order_id
                WHERE o.order_id = ?::uuid
                """, orderId);
        assertEquals(customer.customerId(), persisted.get("id_customer"));
        assertEquals("PENDIENTE", persisted.get("status"));
        assertDecimal("2.000", persisted.get("quantity"));
        assertDecimal("12.00", persisted.get("unit_price"));
        assertEquals("Av. Entregas 200", persisted.get("delivery_address"));
        assertEquals("Contacto Test", persisted.get("delivery_contact"));
        assertEquals("922222222", persisted.get("delivery_phone"));
        assertDecimal("10.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
    }

    @Test
    void insufficientStockRejectsCheckoutWithoutPartialOrderOrStockMutation() throws Exception {
        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("1.000"));
        String jwt = api.login(customer);

        api.post("/api/v1/customer/orders", jwt, Map.of(
                        "items", List.of(Map.of(
                                "productId", product.id().toString(),
                                "quantity", new BigDecimal("2.000")
                        ))
                ))
                .andExpect(status().isConflict());

        assertEquals(0, count("SELECT COUNT(*) FROM customer_order"));
        assertEquals(0, count("SELECT COUNT(*) FROM order_detail"));
        assertEquals(0, count("SELECT COUNT(*) FROM order_transition"));
        assertDecimal("1.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private BigDecimal decimalValue(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, arguments);
    }

    private void assertDecimal(String expected, Object actual) {
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual));
    }
}
