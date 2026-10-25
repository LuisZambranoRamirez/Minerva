-- PostgreSQL exige confirmar el nuevo valor enum antes de utilizarlo.
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'PRODUCT_EXPORT_INVENTORY_REPORT';

INSERT INTO role_permission (id_role, id_permission)
VALUES
    ('ADMIN', 'PRODUCT_EXPORT_INVENTORY_REPORT'),
    ('VENDEDOR', 'PRODUCT_EXPORT_INVENTORY_REPORT'),
    ('ALMACENISTA', 'PRODUCT_EXPORT_INVENTORY_REPORT')
ON CONFLICT (id_role, id_permission) DO NOTHING;
