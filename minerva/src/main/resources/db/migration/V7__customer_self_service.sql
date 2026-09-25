-- Customer self-service foundation: additive/backfill-safe for V1-V6 and existing V6 data.

ALTER TYPE role ADD VALUE IF NOT EXISTS 'CLIENTE';

ALTER TYPE permission ADD VALUE IF NOT EXISTS 'CUSTOMER_CATALOG_READ';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'CUSTOMER_ORDER_CREATE';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'CUSTOMER_ORDER_FIND_BY_ID';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'CUSTOMER_ORDER_FIND_ALL';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'CUSTOMER_ORDER_FIND_TRANSITIONS';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'CUSTOMER_ORDER_CANCEL';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_approval_status') THEN
        CREATE TYPE account_approval_status AS ENUM (
            'PENDING_APPROVAL',
            'APPROVED',
            'REJECTED'
        );
    END IF;
END
$$;

ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS business_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS ruc CHAR(11),
    ADD COLUMN IF NOT EXISTS address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS default_delivery_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS default_delivery_contact VARCHAR(150),
    ADD COLUMN IF NOT EXISTS default_delivery_phone VARCHAR(20);

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS customer_id UUID,
    ADD COLUMN IF NOT EXISTS approval_status account_approval_status NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS approved_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(30),
    ADD COLUMN IF NOT EXISTS rejected_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(30),
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(255);

ALTER TABLE customer_order
    ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_contact VARCHAR(150),
    ADD COLUMN IF NOT EXISTS delivery_phone VARCHAR(20);

ALTER TABLE order_transition
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_app_user_customer'
          AND conrelid = 'app_user'::regclass
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT fk_app_user_customer
            FOREIGN KEY (customer_id)
            REFERENCES customer(customer_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_app_user_approved_by'
          AND conrelid = 'app_user'::regclass
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT fk_app_user_approved_by
            FOREIGN KEY (approved_by)
            REFERENCES app_user(user_name)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_app_user_rejected_by'
          AND conrelid = 'app_user'::regclass
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT fk_app_user_rejected_by
            FOREIGN KEY (rejected_by)
            REFERENCES app_user(user_name)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_customer_ruc_digits'
          AND conrelid = 'customer'::regclass
    ) THEN
        ALTER TABLE customer
            ADD CONSTRAINT chk_customer_ruc_digits
            CHECK (ruc IS NULL OR ruc ~ '^[0-9]{11}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_app_user_approval_metadata'
          AND conrelid = 'app_user'::regclass
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT chk_app_user_approval_metadata
            CHECK (
                (approval_status <> 'APPROVED' OR rejected_date IS NULL)
                AND (approval_status <> 'REJECTED' OR approved_date IS NULL)
            );
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_ruc_not_null
    ON customer (ruc)
    WHERE ruc IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_customer_id_not_null
    ON app_user (customer_id)
    WHERE customer_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_app_user_approval_status_registration_date
    ON app_user (approval_status, registration_date);

CREATE INDEX IF NOT EXISTS idx_customer_order_customer_status_registration_date
    ON customer_order (id_customer, status, registration_date);
