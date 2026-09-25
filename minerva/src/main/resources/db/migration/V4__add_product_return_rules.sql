-- Permisos requeridos por el flujo de devoluciones.
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'SALE_REGISTER_PRODUCT_RETURN';
ALTER TYPE permission ADD VALUE IF NOT EXISTS 'SALE_FIND_PRODUCT_RETURNS';

-- La cantidad de toda devolución debe ser estrictamente positiva.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_product_return_quantity_positive'
          AND conrelid = 'product_return'::regclass
    ) THEN
        ALTER TABLE product_return
            ADD CONSTRAINT chk_product_return_quantity_positive CHECK (quantity > 0);
    END IF;
END
$$;
