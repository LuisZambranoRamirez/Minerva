-- Persist the canonical demo seed identity and prevent duplicate stock-entry ledger rows.

CREATE TABLE demo_catalog_seed (
    seed_key VARCHAR(80) PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE,
    initial_stock_entry_id UUID NOT NULL UNIQUE,
    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_demo_catalog_seed_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_demo_catalog_seed_initial_stock_entry
        FOREIGN KEY (initial_stock_entry_id)
        REFERENCES stock_entry(stock_entry_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uk_inventory_movement_stock_entry_source
    ON inventory_movement (source_id)
    WHERE movement_source = 'STOCK_ENTRY'
      AND source_id IS NOT NULL;
