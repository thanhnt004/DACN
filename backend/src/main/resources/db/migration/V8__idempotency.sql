CREATE TABLE IF NOT EXISTS idempotency_keys (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    key_value text NOT NULL,
    status text NOT NULL,
    response_body jsonb,
    hash text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    expires_at timestamp with time zone NOT NULL,
    CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id)
);

CREATE INDEX ix_idempotency_keys_expires_at ON idempotency_keys(expires_at);
CREATE INDEX ix_idempotency_keys_hash ON idempotency_keys(response_hash);