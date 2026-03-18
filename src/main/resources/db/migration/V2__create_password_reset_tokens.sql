-- V2__create_password_reset_tokens.sql
CREATE TABLE password_reset_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens (user_id);

