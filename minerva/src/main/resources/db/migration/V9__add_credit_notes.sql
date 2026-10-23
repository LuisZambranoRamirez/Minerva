ALTER TYPE permission ADD VALUE IF NOT EXISTS 'SALE_ISSUE_CREDIT_NOTE';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'SALE_FIND_CREDIT_NOTES';

CREATE TABLE IF NOT EXISTS credit_note (
    credit_note_id UUID PRIMARY KEY,
    reference VARCHAR(64) NOT NULL UNIQUE,
    sale_id UUID NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    issued_by VARCHAR(30),
    CONSTRAINT fk_credit_note_sale FOREIGN KEY (sale_id) REFERENCES sale(sale_id) ON DELETE RESTRICT,
    CONSTRAINT fk_credit_note_actor FOREIGN KEY (issued_by) REFERENCES app_user(user_name) ON DELETE RESTRICT,
    CONSTRAINT chk_credit_note_reference CHECK (length(trim(reference)) > 0)
);

ALTER TABLE product_return ADD COLUMN IF NOT EXISTS credit_note_id UUID;

-- Una nota por devolución histórica. El actor es desconocido por diseño para el legado.
INSERT INTO credit_note (credit_note_id, reference, sale_id, issued_at, issued_by)
SELECT pr.product_return_id, 'LEGACY-' || pr.product_return_id,
       sd.id_sale, pr.registration_date, NULL
FROM product_return pr
JOIN sale_detail sd ON sd.sale_detail_id = pr.id_sale_detail
WHERE pr.credit_note_id IS NULL
ON CONFLICT DO NOTHING;

UPDATE product_return
SET credit_note_id = product_return_id
WHERE credit_note_id IS NULL;

ALTER TABLE product_return ALTER COLUMN credit_note_id SET NOT NULL;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_product_return_credit_note') THEN
    ALTER TABLE product_return ADD CONSTRAINT fk_product_return_credit_note
      FOREIGN KEY (credit_note_id) REFERENCES credit_note(credit_note_id) ON DELETE RESTRICT;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_credit_note_sale_issued ON credit_note(sale_id, issued_at);
CREATE INDEX IF NOT EXISTS idx_product_return_credit_note ON product_return(credit_note_id);
