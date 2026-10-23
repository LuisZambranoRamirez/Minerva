-- PostgreSQL exige confirmar ALTER TYPE antes de usar sus valores nuevos.
INSERT INTO role_permission (id_role, id_permission) VALUES
 ('ADMIN', 'SALE_ISSUE_CREDIT_NOTE'), ('ADMIN', 'SALE_FIND_CREDIT_NOTES'),
 ('VENDEDOR', 'SALE_ISSUE_CREDIT_NOTE'), ('VENDEDOR', 'SALE_FIND_CREDIT_NOTES')
ON CONFLICT DO NOTHING;
