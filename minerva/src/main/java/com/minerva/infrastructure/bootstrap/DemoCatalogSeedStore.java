package com.minerva.infrastructure.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DemoCatalogSeedStore {

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    public DemoCatalogSeedStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SeedRecord> findBySeedKey(String seedKey) {
        return jdbcTemplate.query("""
                        SELECT seed_key, product_id, initial_stock_entry_id
                        FROM demo_catalog_seed
                        WHERE seed_key = ?
                        """,
                (resultSet, rowNumber) -> new SeedRecord(
                        resultSet.getString("seed_key"),
                        resultSet.getObject("product_id", UUID.class),
                        resultSet.getObject("initial_stock_entry_id", UUID.class)),
                seedKey).stream().findFirst();
    }

    public void save(String seedKey, UUID productId, UUID initialStockEntryId) {
        entityManager.flush();
        jdbcTemplate.update("""
                INSERT INTO demo_catalog_seed (seed_key, product_id, initial_stock_entry_id)
                VALUES (?, ?, ?)
                """, seedKey, productId, initialStockEntryId);
    }

    public record SeedRecord(String seedKey, UUID productId, UUID initialStockEntryId) {}
}
