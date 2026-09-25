DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_stock_loss_quantity_positive'
          AND conrelid = 'stock_loss'::regclass
    ) THEN
        ALTER TABLE stock_loss
            ADD CONSTRAINT chk_stock_loss_quantity_positive CHECK (quantity > 0);
    END IF;
END
$$;
