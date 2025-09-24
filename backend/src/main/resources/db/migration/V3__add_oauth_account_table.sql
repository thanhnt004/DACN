-- OAuth accounts linking table
CREATE TABLE IF NOT EXISTS oauth_accounts (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider TEXT NOT NULL CHECK (provider IN ('google','github')),
    provider_user_id TEXT NOT NULL,
    email VARCHAR(255),
    display_name TEXT,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    UNIQUE (provider, provider_user_id),
    UNIQUE (user_id, provider)
    );

CREATE INDEX IF NOT EXISTS ix_oauth_accounts_user ON oauth_accounts(user_id);
CREATE INDEX IF NOT EXISTS ix_oauth_accounts_provider ON oauth_accounts(provider, provider_user_id);