ALTER TABLE products
    ADD COLUMN IF NOT EXISTS price BIGINT;
ALTER TABLE product_variants
    DROP COLUMN IF EXISTS concurent;
