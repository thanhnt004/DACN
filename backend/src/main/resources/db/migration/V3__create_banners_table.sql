-- Create banners table
CREATE TABLE IF NOT EXISTS public.banners
(
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    title character varying(255) COLLATE pg_catalog."default" NOT NULL,
    image_url text COLLATE pg_catalog."default" NOT NULL,
    link_url text COLLATE pg_catalog."default",
    description text COLLATE pg_catalog."default",
    display_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    start_date timestamp with time zone,
    end_date timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    deleted_at timestamp with time zone,
    CONSTRAINT banners_pkey PRIMARY KEY (id)
);

-- Create indexes
CREATE INDEX idx_banners_is_active ON public.banners(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_banners_display_order ON public.banners(display_order) WHERE deleted_at IS NULL;
CREATE INDEX idx_banners_dates ON public.banners(start_date, end_date) WHERE deleted_at IS NULL;

-- Add comment
COMMENT ON TABLE public.banners IS 'Banners for homepage and promotional displays';

