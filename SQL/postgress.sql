-- ==========================
-- ENUMS
-- ==========================
CREATE TYPE modifier AS ENUM (
    'FRIO'
);

CREATE TYPE gain_strategy AS ENUM (
    'PORCENTAJE',
    'RECARGO_FIJO'
);

CREATE TYPE sale_type AS ENUM (
    'UNIDAD',
    'GRANEL'
);

CREATE TYPE product_category AS ENUM (
    'BEBIDAS',
    'ABARROTES_SECOS',
    'CAFE_INFUSIONES',
    'LACTEOS',
    'CARNES',
    'SNACKS_GOLOSINAS',
    'CUIDADO_PERSONAL',
    'LIMPIEZA_HOGAR',
    'BEBÉS',
    'MASCOTAS',
    'OTROS'
);

CREATE TYPE stock_loss_reason AS ENUM (
    'DAÑADO',
    'VENCIMIENTO',
    'PERDIDO',
    'ROBO',
    'OTROS'
);

CREATE TYPE role AS ENUM (
    'ADMIN',
    'VENDEDOR',
    'ALMACENISTA'
);

CREATE TYPE permission AS ENUM (
    -- Customer - Write
    'CUSTOMER_REGISTER',
    'CUSTOMER_UPDATE_PHONE_NUMBER',

    -- Customer - Read
    'CUSTOMER_FIND_BY_ID',
    'CUSTOMER_FIND_BY_PHONE_NUMBER',
    'CUSTOMER_GET_ALL',

    -- Product - Write
    'PRODUCT_REGISTER',
    'PRODUCT_REGISTER_STOCK_ENTRY',
    'PRODUCT_ASSOCIATE_UNIT_TO_BULK',

    -- Product - Read
    'PRODUCT_FIND_BY_ID',
    'PRODUCT_FIND_BY_BAR_CODE',
    'PRODUCT_FIND_ALL',

    -- Sale - Write
    'SALE_REGISTER',
    'SALE_ADD_PAYMENT',

    -- Sale - Read
    'SALE_FIND_BY_ID',
    'SALE_FIND_BY_CUSTOMER_ID',
    'SALE_FIND_ALL',

    -- Supplier - Write
    'SUPPLIER_REGISTER',
    'SUPPLIER_UPDATE_PHONE_NUMBER',
    'SUPPLIER_UPDATE_RUC',

    -- Supplier - Read
    'SUPPLIER_FIND_ALL',
    'SUPPLIER_FIND_BY_ID',
    'SUPPLIER_FIND_BY_RUC',
    'SUPPLIER_FIND_BY_PHONE_NUMBER',

    -- User - Write
    'USER_REGISTER',

    -- User - Auth
    'USER_AUTHENTICATE',

    -- User - Read
    'USER_FIND_BY_USERNAME',
    'USER_FIND_BY_ID',
    'USER_FIND_ALL'
);

CREATE TYPE payment_method AS ENUM (
    'EFECTIVO',
    'DIGITAL'
);

CREATE TYPE return_reason AS ENUM (
    'DAÑADO',
    'VENCIDO',
    'EQUIVOCACION',
    'OTROS'
);

-- ==========================
-- TABLAS
-- ==========================

CREATE TABLE personal (
    dni CHAR(8) PRIMARY KEY,
    names VARCHAR(100) NOT NULL,
    lastnames VARCHAR(100) NOT NULL,
    phone_number CHAR(9) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL,
    registration_date TIMESTAMP NOT NULL
);

CREATE TABLE app_user (
    user_name VARCHAR(30) PRIMARY KEY,
    dni CHAR(8) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_name role NOT NULL,
    is_active BOOLEAN NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_app_user_personal
        FOREIGN KEY (dni)
        REFERENCES personal(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ==========================
-- VENDEDOR
-- ==========================

CREATE TABLE seller (
    dni CHAR(8) PRIMARY KEY,

    CONSTRAINT fk_seller_personal
        FOREIGN KEY (dni)
        REFERENCES personal(dni)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ==========================
-- ALMACENISTA
-- ==========================

CREATE TABLE warehouse_keeper (
    dni CHAR(8) PRIMARY KEY,

    CONSTRAINT fk_warehouse_keeper_personal
        FOREIGN KEY (dni)
        REFERENCES personal(dni)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE audit_event (
    audit_event_id UUID PRIMARY KEY,
    user_name VARCHAR(30) NOT NULL,
    permission permission NOT NULL,
    subject_id TEXT NOT NULL,
    subject_name TEXT NOT NULL,
    subject_data JSONB NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_audit_event_user
        FOREIGN KEY (user_name)
        REFERENCES app_user(user_name)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE supplier (
    supplier_id UUID PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL UNIQUE,
    ruc CHAR(11) UNIQUE,
    phone_number CHAR(9) UNIQUE,
    registration_date TIMESTAMP NOT NULL
);

CREATE TABLE customer (
    customer_id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL UNIQUE,
    phone_number CHAR(9) UNIQUE,
    registered_by_seller_dni CHAR(8) NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_customer_seller
        FOREIGN KEY (registered_by_seller_dni)
        REFERENCES seller(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);
CREATE TABLE product (
    product_id UUID PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL UNIQUE,
    gain_strategy gain_strategy NOT NULL,
    gain_amount NUMERIC(10,2) NOT NULL,
    stock NUMERIC(10,3) NOT NULL,
    reorder_level NUMERIC(10,3),
    bar_code CHAR(13) UNIQUE,
    sale_type sale_type NOT NULL,
    category product_category NOT NULL,
    registration_date TIMESTAMP NOT NULL
);

CREATE TABLE unit_to_bulk (
    unit_product_id UUID NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    bulk_product_id UUID NOT NULL UNIQUE,
    registration_date TIMESTAMP NOT NULL,

    PRIMARY KEY (bulk_product_id, unit_product_id),

    CONSTRAINT fk_bulk_product
        FOREIGN KEY (bulk_product_id)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_unit_product
        FOREIGN KEY (unit_product_id)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE stock_entry (
    stock_entry_id UUID PRIMARY KEY,
    id_product UUID NOT NULL,
    id_supplier UUID NOT NULL,
    warehouse_keeper_dni CHAR(8) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    expiration_date TIMESTAMP,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_stock_entry_product
        FOREIGN KEY (id_product)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_stock_entry_supplier
        FOREIGN KEY (id_supplier)
        REFERENCES supplier(supplier_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_stock_entry_warehouse_keeper
        FOREIGN KEY (warehouse_keeper_dni)
        REFERENCES warehouse_keeper(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE sale (
    sale_id UUID PRIMARY KEY,
    id_customer UUID NOT NULL,
    registered_by_seller_dni CHAR(8) NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_sale_customer
        FOREIGN KEY (id_customer)
        REFERENCES customer(customer_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_sale_seller
        FOREIGN KEY (registered_by_seller_dni)
        REFERENCES seller(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE sale_detail (
    sale_detail_id UUID PRIMARY KEY,
    id_sale UUID NOT NULL,
    id_product UUID NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_sale_detail_sale
        FOREIGN KEY (id_sale)
        REFERENCES sale(sale_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_sale_detail_product
        FOREIGN KEY (id_product)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);


CREATE TABLE stock_loss (
    stock_loss_id UUID PRIMARY KEY,
    id_product UUID NOT NULL,
    warehouse_keeper_dni CHAR(8) NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    reason stock_loss_reason NOT NULL,
    observation VARCHAR(255),
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_stock_loss_product
        FOREIGN KEY (id_product)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_stock_loss_warehouse_keeper
        FOREIGN KEY (warehouse_keeper_dni)
        REFERENCES warehouse_keeper(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE pay (
    pay_id UUID PRIMARY KEY,
    id_sale UUID NOT NULL,
    registered_by_seller_dni CHAR(8) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    payment_method payment_method NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_pay_sale
        FOREIGN KEY (id_sale)
        REFERENCES sale(sale_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_pay_seller
        FOREIGN KEY (registered_by_seller_dni)
        REFERENCES seller(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE product_return (
    product_return_id UUID PRIMARY KEY,
    id_sale_detail UUID NOT NULL,
    registered_by_seller_dni CHAR(8) NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    reason return_reason NOT NULL,
    registration_date TIMESTAMP NOT NULL,

    CONSTRAINT fk_product_return_sale_detail
        FOREIGN KEY (id_sale_detail)
        REFERENCES sale_detail(sale_detail_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_product_return_seller
        FOREIGN KEY (registered_by_seller_dni)
        REFERENCES seller(dni)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ==========================
-- PRODUCTO - MODIFICADOR
-- Relación N:M
-- ==========================

CREATE TABLE product_modifier (
    id_product UUID NOT NULL,
    id_modifier_name modifier NOT NULL,

    extra_price NUMERIC(10,2) NOT NULL,

    registration_date TIMESTAMP NOT NULL,

    PRIMARY KEY (id_product, id_modifier_name),

    CONSTRAINT fk_product_modifier_product
        FOREIGN KEY (id_product)
        REFERENCES product(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);


-- ==========================
-- MODIFICADORES DE LA VENTA
-- ==========================

CREATE TABLE sale_detail_modifier (
    id_sale_detail UUID NOT NULL,
    id_modifier_name modifier NOT NULL,

    quantity NUMERIC(10,3) NOT NULL,
    extra_price NUMERIC(10,2) NOT NULL,

    PRIMARY KEY (id_sale_detail, id_modifier_name),

    CONSTRAINT fk_sale_detail_modifier_detail
        FOREIGN KEY (id_sale_detail)
        REFERENCES sale_detail(sale_detail_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ==========================
-- ROL - PERMISO
-- Relación N:M
-- ==========================

CREATE TABLE role_permission (
    id_role role NOT NULL,
    id_permission permission NOT NULL,

    PRIMARY KEY (id_role, id_permission)
);

-- ==========================
-- USUARIO - PERMISO
-- Permisos específicos
-- ==========================

CREATE TABLE user_permission (
    id_user VARCHAR(30) NOT NULL,
    id_permission permission NOT NULL,

    is_allowed BOOLEAN NOT NULL,

    registration_date TIMESTAMP NOT NULL,

    PRIMARY KEY (id_user, id_permission),

    CONSTRAINT fk_user_permission_user
        FOREIGN KEY (id_user)
        REFERENCES app_user(user_name)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);
-- ==========================
-- DATOS INICIALES
-- ==========================

INSERT INTO supplier (supplier_id, supplier_name, registration_date)
VALUES (gen_random_uuid(), 'anonimo', NOW());

INSERT INTO customer (customer_id, full_name, registration_date)
VALUES (gen_random_uuid(), 'anonimo', NOW());

-- ==========================
-- AUDITORIA 
-- AJENO A LAS REGLAS DE NEGOCIO, EN CONSECUENCIA SE TOMARON MAS LIBERTADES CON RESPECTO AL TRATAMIENTO DE LOS DATOS
-- ==========================

CREATE TABLE exception_log (
    id              BIGSERIAL PRIMARY KEY,
    exception_type  TEXT NOT NULL,
    message         TEXT,
    cause_type      TEXT,
    cause_message   TEXT,
    stack_trace     TEXT,
    occurred_at     TIMESTAMP NOT NULL
);