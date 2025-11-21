ALTER TABLE public.addresses
    DROP CONSTRAINT IF EXISTS user_default_add_unique;

CREATE UNIQUE INDEX IF NOT EXISTS idx_addresses_default_per_user
    ON public.addresses (user_id)
    WHERE is_default_shipping = true;
