CREATE TABLE shared_pastes (
    id               BIGSERIAL PRIMARY KEY,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    exposure         VARCHAR(16),
    password         VARCHAR(255),
    title            VARCHAR(255),
    expiration_date  TIMESTAMP,
    content          TEXT,
    url              VARCHAR(255) NOT NULL UNIQUE,
    language         VARCHAR(64),
    views            BIGINT       NOT NULL DEFAULT 0,
    burn_after_read  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_shared_pastes_exposure_expiration
    ON shared_pastes (exposure, expiration_date);
