package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnumFilterIntegrationTest extends IntegrationTest {

    @Test
    void orderAndInventoryFiltersBindPostgresNamedEnumsAndReturnOnlyExactMatches() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        String jwt = api.login(seller);

        List<String> statuses = List.of(
                "PENDIENTE", "CONFIRMADO", "EN_PREPARACION", "EN_REPARTO", "ENTREGADO", "CANCELADO");
        for (String orderStatus : statuses) {
            UUID orderId = stableId("order-" + orderStatus);
            UUID detailId = stableId("order-detail-" + orderStatus);
            fixtures.order(orderId, detailId, customer, product, orderStatus);

            assertEquals(orderStatus, jdbcTemplate.queryForObject(
                    "SELECT CAST(? AS order_status)::text", String.class, orderStatus));
            api.get("/api/v1/orders?status=" + orderStatus.toLowerCase(Locale.ROOT), jwt)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$[0].status").value(orderStatus));
        }

        Map<String, String> movementSources = new LinkedHashMap<>();
        movementSources.put("STOCK_ENTRY", "ENTRADA");
        movementSources.put("ORDER_CONFIRMATION", "SALIDA");
        movementSources.put("ORDER_CANCELLATION", "ENTRADA");
        movementSources.put("INVENTORY_LOSS", "SALIDA");
        movementSources.put("PRODUCT_RETURN", "ENTRADA");
        movementSources.put("DIRECT_SALE", "SALIDA");

        int index = 0;
        for (Map.Entry<String, String> entry : movementSources.entrySet()) {
            String source = entry.getKey();
            String type = entry.getValue();
            UUID sourceId = stableId("movement-source-" + source);
            boolean incoming = "ENTRADA".equals(type);
            insertMovement(
                    product.id(),
                    seller.username(),
                    incoming ? "1.000" : "-1.000",
                    incoming ? "9.000" : "10.000",
                    incoming ? "10.000" : "9.000",
                    type,
                    source,
                    sourceId,
                    LocalDateTime.of(2026, 1, 15, 11, index++)
            );

            assertEquals(type, jdbcTemplate.queryForObject(
                    "SELECT CAST(? AS inventory_movement_type)::text", String.class, type));
            assertEquals(source, jdbcTemplate.queryForObject(
                    "SELECT CAST(? AS inventory_movement_source)::text", String.class, source));
            api.get("/api/v1/products/inventory-movements?source=" + source.toLowerCase(Locale.ROOT), jwt)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].type").value(type))
                    .andExpect(jsonPath("$[0].source").value(source))
                    .andExpect(jsonPath("$[0].sourceId").value(sourceId.toString()));
        }

        api.get("/api/v1/products/inventory-movements?type=entrada", jwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].source").value("PRODUCT_RETURN"))
                .andExpect(jsonPath("$[1].source").value("ORDER_CANCELLATION"))
                .andExpect(jsonPath("$[2].source").value("STOCK_ENTRY"));
        api.get("/api/v1/products/inventory-movements?type=SALIDA", jwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].source").value("DIRECT_SALE"))
                .andExpect(jsonPath("$[1].source").value("INVENTORY_LOSS"))
                .andExpect(jsonPath("$[2].source").value("ORDER_CONFIRMATION"));

        assertEquals(statuses, enumLabels("order_status"));
        assertEquals(List.of("ENTRADA", "SALIDA"), enumLabels("inventory_movement_type"));
        assertEquals(List.copyOf(movementSources.keySet()), enumLabels("inventory_movement_source"));
    }

    private UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private void insertMovement(
            UUID productId,
            String actor,
            String quantity,
            String stockBefore,
            String stockAfter,
            String type,
            String source,
            UUID sourceId,
            LocalDateTime registrationDate
    ) {
        jdbcTemplate.update("""
                INSERT INTO inventory_movement (
                    inventory_movement_id, id_product, quantity, stock_before, stock_after,
                    movement_type, movement_source, source_id, actor_user_name, registration_date
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS inventory_movement_type),
                          CAST(? AS inventory_movement_source), ?, ?, ?)
                """,
                UUID.randomUUID(), productId, new BigDecimal(quantity), new BigDecimal(stockBefore),
                new BigDecimal(stockAfter), type, source, sourceId, actor, registrationDate);
    }

    private List<String> enumLabels(String enumName) {
        return jdbcTemplate.queryForList("""
                SELECT enumlabel
                FROM pg_enum
                JOIN pg_type ON pg_type.oid = pg_enum.enumtypid
                WHERE pg_type.typname = ?
                ORDER BY enumsortorder
                """, String.class, enumName);
    }
}
