-- Products: counters/flags
ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS sold_count INT NOT NULL DEFAULT 0;

ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS rating_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rating_sum INT NOT NULL DEFAULT 0;

-- Generated average rating (PG12+)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='products' AND column_name='rating_avg'
  ) THEN
    EXECUTE $gen$
ALTER TABLE public.products
    ADD COLUMN rating_avg NUMERIC(3,2)
        GENERATED ALWAYS AS (
            CASE
                WHEN rating_count > 0 THEN round(rating_sum::numeric / GREATEST(rating_count,1), 2)
                ELSE 0
                END
            ) STORED
    $gen$;
END IF;
END$$;

ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS price_min_amount BIGINT,
    ADD COLUMN IF NOT EXISTS price_max_amount BIGINT,
    ADD COLUMN IF NOT EXISTS total_stock INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_in_stock BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS primary_image_url TEXT;

-- Product variants: quick flags
ALTER TABLE public.product_variants
    ADD COLUMN IF NOT EXISTS sold_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_in_stock BOOLEAN NOT NULL DEFAULT FALSE;

-- Categories/brands: facet counts
ALTER TABLE public.categories
    ADD COLUMN IF NOT EXISTS products_count INT NOT NULL DEFAULT 0;

ALTER TABLE public.brands
    ADD COLUMN IF NOT EXISTS products_count INT NOT NULL DEFAULT 0;

-- Orders: milestone for sales count windows
ALTER TABLE public.orders
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- Indexes for common sorts/filters
CREATE INDEX IF NOT EXISTS ix_products_sold_desc
    ON public.products (status, sold_count DESC);

CREATE INDEX IF NOT EXISTS ix_products_rating_desc
    ON public.products (status, rating_avg DESC, rating_count DESC);

CREATE INDEX IF NOT EXISTS ix_products_price_min
    ON public.products (status, is_in_stock DESC, price_min_amount);

CREATE INDEX IF NOT EXISTS ix_products_brand_sold
    ON public.products (brand_id, status, sold_count DESC);

-- Category filtering (optimize for category -> product join)
CREATE INDEX IF NOT EXISTS ix_product_categories_category_product
    ON public.product_categories (category_id, product_id);

-- Variants
CREATE INDEX IF NOT EXISTS ix_product_variants_product_status_price
    ON public.product_variants (product_id, status, price_amount);
