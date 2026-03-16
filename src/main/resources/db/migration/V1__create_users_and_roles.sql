-- V1__create_users_and_roles.sql
CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   TEXT         NOT NULL,
    enabled    BOOLEAN      NOT NULL    DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE user_roles
(
    user_id BIGINT       NOT NULL,
    role    VARCHAR(100) NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_role UNIQUE (user_id, role)
);

-- indexes
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);