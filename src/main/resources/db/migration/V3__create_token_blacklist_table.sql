CREATE TABLE token_blacklist (
    id          UUID    PRIMARY KEY,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_token_blacklist_hash ON token_blacklist (token_hash);
