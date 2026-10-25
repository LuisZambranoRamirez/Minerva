package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryMovementCorrelationMigrationStaticTest {

    @Test
    void v13CreatesSeedManifestAndUniqueStockEntryCorrelation() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V13__correlate_stock_entry_movements.sql"));

        assertThat(sql)
                .contains("CREATE TABLE demo_catalog_seed")
                .contains("seed_key VARCHAR(80) PRIMARY KEY")
                .contains("product_id UUID NOT NULL UNIQUE")
                .contains("initial_stock_entry_id UUID NOT NULL UNIQUE")
                .contains("REFERENCES product(product_id)")
                .contains("REFERENCES stock_entry(stock_entry_id)")
                .contains("CREATE UNIQUE INDEX uk_inventory_movement_stock_entry_source")
                .contains("ON inventory_movement (source_id)")
                .contains("movement_source = 'STOCK_ENTRY'")
                .contains("source_id IS NOT NULL");
    }
}
