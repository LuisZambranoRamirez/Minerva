package com.minerva.integration;

import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMovementCorrelationIntegrationTest extends IntegrationTest {

    @Autowired
    private InventoryMovementRepository movements;

    @Test
    void stockEntryCorrelationIsIdempotentAndDatabaseEnforcesUniqueness() throws DomainException {
        IntegrationFixtures.UserFixture actor = fixtures.warehouseUser();
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        UUID entryId = createStockEntry(product.id());
        InventoryMovement movement = stockEntryMovement(product.id(), entryId, actor.username());

        assertFalse(movements.existsBySourceAndSourceId(InventoryMovementSource.STOCK_ENTRY, entryId));
        assertTrue(movements.appendIfAbsent(movement));
        assertTrue(movements.existsBySourceAndSourceId(InventoryMovementSource.STOCK_ENTRY, entryId));
        assertFalse(movements.appendIfAbsent(stockEntryMovement(product.id(), entryId, actor.username())));
        assertEquals(1, countMovements(entryId));

        assertThrows(DataIntegrityViolationException.class, () -> insertDuplicateMovement(product.id(), entryId,
                actor.username()));
        assertEquals(1, countMovements(entryId));
    }

    @Test
    void seedManifestUniquelyIdentifiesCanonicalProductAndEntry() {
        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("10.000"));
        UUID entryId = createStockEntry(product.id());

        jdbcTemplate.update("""
                INSERT INTO demo_catalog_seed (seed_key, product_id, initial_stock_entry_id)
                VALUES (?, ?, ?)
                """, "MINV-0001-TEST", product.id(), entryId);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO demo_catalog_seed (seed_key, product_id, initial_stock_entry_id)
                VALUES (?, ?, ?)
                """, "ANOTHER-SEED", product.id(), entryId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_catalog_seed", Integer.class));
    }

    private UUID createStockEntry(UUID productId) {
        UUID receiptId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID supplierId = jdbcTemplate.queryForObject(
                "SELECT supplier_id FROM supplier WHERE supplier_name = 'anonimo'", UUID.class);
        jdbcTemplate.update("""
                INSERT INTO stock_receipt (stock_receipt_id, id_supplier, registration_date)
                VALUES (?, ?, ?)
                """, receiptId, supplierId, LocalDateTime.now());
        jdbcTemplate.update("""
                INSERT INTO stock_entry (
                    stock_entry_id, id_stock_receipt, id_product, unit_price, quantity, expiration_date
                ) VALUES (?, ?, ?, ?, ?, NULL)
                """, entryId, receiptId, productId, new BigDecimal("10.00"), new BigDecimal("10.000"));
        return entryId;
    }

    private InventoryMovement stockEntryMovement(UUID productId, UUID entryId, String actor)
            throws DomainException {
        return new InventoryMovement(
                new ProductIdImpl(productId),
                new BigDecimal("10.000"),
                new ProductQuantity(BigDecimal.ZERO),
                new ProductQuantity(new BigDecimal("10.000")),
                InventoryMovementSource.STOCK_ENTRY,
                entryId,
                new UserName(actor));
    }

    private void insertDuplicateMovement(UUID productId, UUID entryId, String actor) {
        jdbcTemplate.update("""
                INSERT INTO inventory_movement (
                    inventory_movement_id, id_product, quantity, stock_before, stock_after,
                    movement_type, movement_source, source_id, actor_user_name, registration_date
                ) VALUES (?, ?, ?, ?, ?, CAST('ENTRADA' AS inventory_movement_type),
                          CAST('STOCK_ENTRY' AS inventory_movement_source), ?, ?, ?)
                """, UUID.randomUUID(), productId, new BigDecimal("10.000"), BigDecimal.ZERO,
                new BigDecimal("10.000"), entryId, actor, LocalDateTime.now());
    }

    private int countMovements(UUID entryId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_movement
                WHERE movement_source = CAST('STOCK_ENTRY' AS inventory_movement_source)
                  AND source_id = ?
                """, Integer.class, entryId);
        return count == null ? 0 : count;
    }
}
