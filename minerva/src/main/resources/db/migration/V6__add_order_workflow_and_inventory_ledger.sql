-- Add order workflow and inventory ledger schema.

CREATE TYPE order_status AS ENUM (
    'PENDIENTE',
    'CONFIRMADO',
    'EN_PREPARACION',
    'EN_REPARTO',
    'ENTREGADO',
    'CANCELADO'
);

CREATE TYPE inventory_movement_type AS ENUM (
    'ENTRADA',
    'SALIDA'
);

CREATE TYPE inventory_movement_source AS ENUM (
    'STOCK_ENTRY',
    'ORDER_CONFIRMATION',
    'ORDER_CANCELLATION',
    'INVENTORY_LOSS',
    'PRODUCT_RETURN',
    'DIRECT_SALE'
);

ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_CREATE';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_CONFIRM';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_PREPARE';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_DISPATCH';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_DELIVER';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_CANCEL';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_FIND_BY_ID';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_FIND_ALL';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'ORDER_FIND_TRANSITIONS';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'PRODUCT_FIND_LOW_STOCK';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'PRODUCT_FIND_INVENTORY_MOVEMENTS';

CREATE TABLE customer_order (
    order_id UUID PRIMARY KEY,
    id_customer UUID NOT NULL,
    status order_status NOT NULL,
    registration_date TIMESTAMP NOT NULL,
    updated_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_customer_order_customer
        FOREIGN KEY (id_customer)
        REFERENCES customer(customer_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE order_detail (
    order_detail_id UUID PRIMARY KEY,
    id_order UUID NOT NULL,
    id_product UUID NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,

    CONSTRAINT chk_order_detail_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_detail_unit_price_positive CHECK (unit_price > 0),

    CONSTRAINT uk_order_detail_order_product UNIQUE (id_order, id_product),

    CONSTRAINT fk_order_detail_order
        FOREIGN KEY (id_order)
        REFERENCES customer_order(order_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_order_detail_product
        FOREIGN KEY (id_product)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE order_transition (
    order_transition_id UUID PRIMARY KEY,
    id_order UUID NOT NULL,
    previous_status order_status,
    new_status order_status NOT NULL,
    actor_user_name VARCHAR(30) NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_order_transition_order
        FOREIGN KEY (id_order)
        REFERENCES customer_order(order_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_order_transition_actor
        FOREIGN KEY (actor_user_name)
        REFERENCES app_user(user_name)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE inventory_movement (
    inventory_movement_id UUID PRIMARY KEY,
    id_product UUID NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    stock_before NUMERIC(10,3) NOT NULL,
    stock_after NUMERIC(10,3) NOT NULL,
    movement_type inventory_movement_type NOT NULL,
    movement_source inventory_movement_source NOT NULL,
    source_id UUID,
    actor_user_name VARCHAR(30) NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT chk_inventory_movement_quantity_not_zero CHECK (quantity <> 0),
    CONSTRAINT chk_inventory_movement_stock_before_non_negative CHECK (stock_before >= 0),
    CONSTRAINT chk_inventory_movement_stock_after_non_negative CHECK (stock_after >= 0),
    CONSTRAINT chk_inventory_movement_type_sign CHECK (
        (movement_type = 'ENTRADA' AND quantity > 0)
        OR (movement_type = 'SALIDA' AND quantity < 0)
    ),

    CONSTRAINT fk_inventory_movement_product
        FOREIGN KEY (id_product)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_inventory_movement_actor
        FOREIGN KEY (actor_user_name)
        REFERENCES app_user(user_name)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

ALTER TABLE sale
    ADD COLUMN source_order_id UUID;

ALTER TABLE sale
    ADD CONSTRAINT uk_sale_source_order UNIQUE (source_order_id);

ALTER TABLE sale
    ADD CONSTRAINT fk_sale_source_order
        FOREIGN KEY (source_order_id)
        REFERENCES customer_order(order_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE;

CREATE INDEX idx_customer_order_status_registration_date
    ON customer_order (status, registration_date);

CREATE INDEX idx_customer_order_customer_registration_date
    ON customer_order (id_customer, registration_date);

CREATE INDEX idx_order_transition_order_registration_date
    ON order_transition (id_order, registration_date);

CREATE INDEX idx_order_detail_product
    ON order_detail (id_product);

CREATE INDEX idx_inventory_movement_product_registration_date
    ON inventory_movement (id_product, registration_date);

CREATE INDEX idx_inventory_movement_type_source_registration_date
    ON inventory_movement (movement_type, movement_source, registration_date);

CREATE INDEX idx_product_low_stock
    ON product (stock, reorder_level)
    WHERE reorder_level IS NOT NULL;