-- Seed CLIENTE role permissions after V7 enum values are committed.
-- PostgreSQL does not allow using enum values added in the same transaction,
-- so this data migration is intentionally separated from V7.

INSERT INTO role_permission (id_role, id_permission)
VALUES
    ('CLIENTE', 'CUSTOMER_CATALOG_READ'),
    ('CLIENTE', 'CUSTOMER_ORDER_CREATE'),
    ('CLIENTE', 'CUSTOMER_ORDER_FIND_BY_ID'),
    ('CLIENTE', 'CUSTOMER_ORDER_FIND_ALL'),
    ('CLIENTE', 'CUSTOMER_ORDER_FIND_TRANSITIONS'),
    ('CLIENTE', 'CUSTOMER_ORDER_CANCEL')
ON CONFLICT (id_role, id_permission) DO NOTHING;

CREATE TABLE IF NOT EXISTS abuse_attempt (
    abuse_attempt_id UUID PRIMARY KEY,
    action VARCHAR(40) NOT NULL,
    subject_key VARCHAR(120) NOT NULL,
    successful BOOLEAN NOT NULL,
    registration_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_abuse_attempt_action_subject_date
    ON abuse_attempt (action, subject_key, registration_date);