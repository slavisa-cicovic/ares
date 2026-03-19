CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    replaced_by_token_id UUID NULL REFERENCES refresh_tokens (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);

CREATE INDEX ix_refresh_tokens_user_id
    ON refresh_tokens (user_id);
