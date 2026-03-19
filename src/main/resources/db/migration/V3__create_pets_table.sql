-- V3__create_pets_table.sql
CREATE TABLE pets
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    species     VARCHAR(50)  NOT NULL,
    breed       VARCHAR(100),
    age         INT,
    description TEXT,
    image_url   TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_pet_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_pets_user_id ON pets (user_id);
