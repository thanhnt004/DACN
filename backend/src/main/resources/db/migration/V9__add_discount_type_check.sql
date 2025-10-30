-- Add CHECK constraint for discount type
ALTER TABLE discounts
    DROP CONSTRAINT IF EXISTS discounts_type_check;

ALTER TABLE discounts
    ADD CONSTRAINT discounts_type_check CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT'));
