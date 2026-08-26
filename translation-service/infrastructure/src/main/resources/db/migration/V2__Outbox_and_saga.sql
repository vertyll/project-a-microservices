-- Messaging tables required by shared-infrastructure.
--
-- translation-service publishes no events, but the outbox processor and the saga
-- engine are wired by the shared autoconfiguration in every service, so
-- Hibernate validates their entities on start-up regardless.
--
-- Copied verbatim from project-service rather than written afresh: these tables
-- are the shared module's contract, and a handwritten variant is how column
-- names drift apart.

CREATE TABLE IF NOT EXISTS kafka_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    payload BYTEA NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP NULL,
    saga_id VARCHAR(255) NULL,
    version BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_status ON kafka_outbox (status);

CREATE TABLE IF NOT EXISTS saga (
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
CREATE INDEX IF NOT EXISTS idx_saga_status ON saga (status);

CREATE TABLE IF NOT EXISTS saga_step (
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

    CONSTRAINT uk_saga_step UNIQUE (saga_id, step_name),
    CONSTRAINT fk_saga_step_saga FOREIGN KEY (saga_id) REFERENCES saga(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_saga_step_saga_id ON saga_step (saga_id);

CREATE TABLE IF NOT EXISTS processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_processed_event_event_id_consumer UNIQUE (event_id, consumer_group)
);
CREATE INDEX IF NOT EXISTS idx_processed_event_processed_at ON processed_event (processed_at);
