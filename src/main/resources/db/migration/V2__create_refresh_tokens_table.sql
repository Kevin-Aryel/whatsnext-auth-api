CREATE TABLE refresh_tokens (
    id         UUID    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    user_id    UUID    NOT NULL REFERENCES users (id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
