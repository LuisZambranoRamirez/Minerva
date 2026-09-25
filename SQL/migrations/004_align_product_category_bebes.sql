DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum enum_value
        JOIN pg_type enum_type ON enum_type.oid = enum_value.enumtypid
        WHERE enum_type.typname = 'product_category'
          AND enum_value.enumlabel = 'BEBÉS'
    ) AND NOT EXISTS (
        SELECT 1
        FROM pg_enum enum_value
        JOIN pg_type enum_type ON enum_type.oid = enum_value.enumtypid
        WHERE enum_type.typname = 'product_category'
          AND enum_value.enumlabel = 'BEBES'
    ) THEN
        ALTER TYPE product_category RENAME VALUE 'BEBÉS' TO 'BEBES';
    END IF;
END
$$;
