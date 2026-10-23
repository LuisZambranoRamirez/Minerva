package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends IntegrationTest {

    @Test
    void customerCatalogIsAuthenticatedDeterministicReadOnlyAndOmitsSensitiveInventoryFields() throws Exception {
        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        fixtures.product(new BigDecimal("5.000"));
        fixtures.secondProduct(BigDecimal.ZERO);
        String jwt = api.login(customer);

        api.get("/api/v1/customer/catalog")
                .andExpect(status().isUnauthorized());

        long productsBefore = longValue("SELECT COUNT(*) FROM product");
        BigDecimal stockBefore = decimalValue("SELECT SUM(stock) FROM product");

        api.get("/api/v1/customer/catalog", jwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Alfa Integración"))
                .andExpect(jsonPath("$[0].availability").value("AGOTADO"))
                .andExpect(jsonPath("$[1].productName").value("Producto Integración"))
                .andExpect(jsonPath("$[1].availability").value("DISPONIBLE"))
                .andExpect(jsonPath("$[0].price").value(12.0))
                .andExpect(jsonPath("$[0].cost").doesNotExist())
                .andExpect(jsonPath("$[0].stock").doesNotExist())
                .andExpect(jsonPath("$[0].gainAmount").doesNotExist())
                .andExpect(jsonPath("$[0].reorderLevel").doesNotExist());

        assertEquals(productsBefore, longValue("SELECT COUNT(*) FROM product"));
        assertEquals(0, stockBefore.compareTo(decimalValue("SELECT SUM(stock) FROM product")));
    }

    private long longValue(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private BigDecimal decimalValue(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }
}
