package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalesPaymentReturnIntegrationTest extends IntegrationTest {

    @Test
    void saleAdditionalPaymentAndOverpaymentKeepFinancialAndInventoryStateAtomic() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        String jwt = api.login(seller);

        api.post("/api/v1/sales", jwt, Map.of(
                        "customerId", customer.id().toString(),
                        "items", List.of(Map.of(
                                "productId", product.id().toString(),
                                "quantity", new BigDecimal("4.000")
                        )),
                        "payments", List.of(Map.of(
                                "amount", new BigDecimal("10.00"),
                                "paymentMethod", "EFECTIVO"
                        ))
                ))
                .andExpect(status().isCreated());

        UUID saleId = jdbcTemplate.queryForObject("SELECT sale_id FROM sale", UUID.class);
        api.get("/api/v1/sales/" + saleId, jwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(48.0))
                .andExpect(jsonPath("$.totalPaid").value(10.0))
                .andExpect(jsonPath("$.amountDue").value(38.0))
                .andExpect(jsonPath("$.details[0].unitPrice").value(12.0));
        assertDecimal("6.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount("DIRECT_SALE"));

        api.post("/api/v1/sales/" + saleId + "/payments", jwt, Map.of(
                        "payments", List.of(Map.of(
                                "amount", new BigDecimal("20.00"),
                                "paymentMethod", "DIGITAL"
                        ))
                ))
                .andExpect(status().isOk());
        assertEquals(2, count("SELECT COUNT(*) FROM pay"));
        assertDecimal("30.00", decimalValue("SELECT SUM(amount) FROM pay"));

        api.post("/api/v1/sales/" + saleId + "/payments", jwt, Map.of(
                        "payments", List.of(Map.of(
                                "amount", new BigDecimal("18.01"),
                                "paymentMethod", "EFECTIVO"
                        ))
                ))
                .andExpect(status().isBadRequest());
        assertEquals(2, count("SELECT COUNT(*) FROM pay"));
        assertDecimal("30.00", decimalValue("SELECT SUM(amount) FROM pay"));
        assertDecimal("6.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
    }

    @Test
    void cumulativeReturnsCannotExceedSoldQuantityAndOnlyEquivocacionRestoresStockOnce() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        String jwt = api.login(seller);
        api.post("/api/v1/sales", jwt, Map.of(
                        "customerId", customer.id().toString(),
                        "items", List.of(Map.of(
                                "productId", product.id().toString(),
                                "quantity", new BigDecimal("4.000")
                        )),
                        "payments", List.of()
                ))
                .andExpect(status().isCreated());
        UUID detailId = jdbcTemplate.queryForObject("SELECT sale_detail_id FROM sale_detail", UUID.class);

        api.post("/api/v1/sales/details/" + detailId + "/returns", jwt,
                        Map.of("quantity", new BigDecimal("1.000"), "reason", "DAÑADO"))
                .andExpect(status().isCreated());
        assertDecimal("6.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(0, movementCount("PRODUCT_RETURN"));

        api.post("/api/v1/sales/details/" + detailId + "/returns", jwt,
                        Map.of("quantity", new BigDecimal("2.000"), "reason", "EQUIVOCACION"))
                .andExpect(status().isCreated());
        assertDecimal("8.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount("PRODUCT_RETURN"));
        assertDecimal("2.000", decimalValue("""
                SELECT quantity FROM inventory_movement WHERE movement_source = 'PRODUCT_RETURN'
                """));

        api.post("/api/v1/sales/details/" + detailId + "/returns", jwt,
                        Map.of("quantity", new BigDecimal("2.000"), "reason", "EQUIVOCACION"))
                .andExpect(status().isBadRequest());

        assertEquals(2, count("SELECT COUNT(*) FROM product_return"));
        assertDecimal("3.000", decimalValue("SELECT SUM(quantity) FROM product_return"));
        assertDecimal("8.000", decimalValue("SELECT stock FROM product WHERE product_id = ?", product.id()));
        assertEquals(1, movementCount("PRODUCT_RETURN"));
    }

    private int movementCount(String source) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_movement
                WHERE movement_source = CAST(? AS inventory_movement_source)
                """, Integer.class, source);
        return value == null ? 0 : value;
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
