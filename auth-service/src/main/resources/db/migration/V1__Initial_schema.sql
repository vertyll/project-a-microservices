-- Flyway initial schema for auth-service based on JPA entities
-- Database: PostgreSQL

-- ===============
-- kafka_outbox (shared)
CREATE TABLE IF NOT EXISTS kafka_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    saga_id VARCHAR(255) NULL,
    version BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_status ON kafka_outbox (status);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_created_at ON kafka_outbox (created_at);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_topic ON kafka_outbox (topic);

-- ===============
-- auth_user
CREATE TABLE IF NOT EXISTS auth_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_auth_user_email UNIQUE (email)
);
CREATE INDEX IF NOT EXISTS idx_auth_user_email ON auth_user (email);
CREATE INDEX IF NOT EXISTS idx_auth_user_enabled ON auth_user (enabled);
CREATE INDEX IF NOT EXISTS idx_auth_user_created_at ON auth_user (created_at);

-- ===============
-- auth_user_role
CREATE TABLE IF NOT EXISTS auth_user_role (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_auth_user_role UNIQUE (auth_user_id, role_id),
    CONSTRAINT fk_auth_user_role_user FOREIGN KEY (auth_user_id) REFERENCES auth_user(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_auth_user_role_user_id ON auth_user_role (auth_user_id);
CREATE INDEX IF NOT EXISTS idx_auth_user_role_role_id ON auth_user_role (role_id);

-- ===============
-- refresh_token
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    device_info TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_refresh_token_token UNIQUE (token)
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_username ON refresh_token (username);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expiry_date ON refresh_token (expiry_date);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON refresh_token (revoked);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_token (token);

-- ===============
-- verification_token
CREATE TABLE IF NOT EXISTS verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    token_type VARCHAR(255) NOT NULL,
    additional_data TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_verification_token_token UNIQUE (token)
);
CREATE INDEX IF NOT EXISTS idx_verification_token_username ON verification_token (username);
CREATE INDEX IF NOT EXISTS idx_verification_token_expiry_date ON verification_token (expiry_date);
CREATE INDEX IF NOT EXISTS idx_verification_token_used ON verification_token (used);
CREATE INDEX IF NOT EXISTS idx_verification_token_token_type ON verification_token (token_type);
CREATE INDEX IF NOT EXISTS idx_verification_token_token ON verification_token (token);

-- ===============
-- auth_saga
CREATE TABLE IF NOT EXISTS auth_saga (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    last_error TEXT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_auth_saga_status ON auth_saga (status);
CREATE INDEX IF NOT EXISTS idx_auth_saga_type ON auth_saga (type);
CREATE INDEX IF NOT EXISTS idx_auth_saga_started_at ON auth_saga (started_at);

-- ===============
-- auth_saga_step
CREATE TABLE IF NOT EXISTS auth_saga_step (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(255) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    compensation_step_id BIGINT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_auth_saga_step UNIQUE (saga_id, step_name),
    CONSTRAINT fk_auth_saga_step_saga FOREIGN KEY (saga_id) REFERENCES auth_saga(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_auth_saga_step_saga_id ON auth_saga_step (saga_id);
CREATE INDEX IF NOT EXISTS idx_auth_saga_step_status ON auth_saga_step (status);
