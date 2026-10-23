package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersistenceConstraintIntegrationTest extends IntegrationTest {

    @Test
    void orderDetailUniquenessAndPositiveQuantityConstraintsRejectWithoutPartialRows() {
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        IntegrationFixtures.ProductFixture secondProduct = fixtures.secondProduct(new BigDecimal("10.000"));
        IntegrationFixtures.OrderFixture order = fixtures.order(customer, product);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO order_detail (order_detail_id, id_order, id_product, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), order.id(), product.id(), new BigDecimal("1.000"), new BigDecimal("12.00")));
        assertEquals(1, count("SELECT COUNT(*) FROM order_detail"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO order_detail (order_detail_id, id_order, id_product, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), order.id(), secondProduct.id(),
                new BigDecimal("-1.000"), new BigDecimal("12.00")));
        assertEquals(1, count("SELECT COUNT(*) FROM order_detail"));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement"));
    }

    @Test
    void inventoryMovementSignAndForeignKeyConstraintsRejectInvalidRelationalState() {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO inventory_movement (
                    inventory_movement_id, id_product, quantity, stock_before, stock_after,
                    movement_type, movement_source, source_id, actor_user_name, registration_date
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS inventory_movement_type),
                          CAST(? AS inventory_movement_source), ?, ?, ?)
                """,
                UUID.randomUUID(), product.id(), new BigDecimal("1.000"), new BigDecimal("10.000"),
                new BigDecimal("9.000"), "SALIDA", "ORDER_CONFIRMATION", UUID.randomUUID(),
                seller.username(), LocalDateTime.now()));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO customer_order (
                    order_id, id_customer, status, registration_date, updated_date
                ) VALUES (?, ?, CAST(? AS order_status), ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), "PENDIENTE", LocalDateTime.now(), LocalDateTime.now()));
        assertEquals(0, count("SELECT COUNT(*) FROM customer_order"));
    }

    @Test
    void customerRucAndApprovalMetadataConstraintsRejectInvalidRowsAtomically() {
        IntegrationFixtures.CustomerFixture customer = fixtures.customer();
        IntegrationFixtures.UserFixture admin = fixtures.admin();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO customer (
                    customer_id, full_name, phone_number, registration_date,
                    business_name, legal_name, ruc, address
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), "Cliente RUC duplicado", "911111119", LocalDateTime.now(),
                "Negocio duplicado", "Empresa duplicada SAC", "20600000001", "Av. Duplicada 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM customer WHERE full_name <> 'anonimo'"));
        assertEquals(customer.id(), jdbcTemplate.queryForObject(
                "SELECT customer_id FROM customer WHERE ruc = '20600000001'", UUID.class));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                UPDATE app_user
                SET approval_status = 'REJECTED', approved_date = NOW()
                WHERE user_name = ?
                """, admin.username()));
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "SELECT approval_status::text FROM app_user WHERE user_name = ?",
                String.class,
                admin.username()
        ));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
