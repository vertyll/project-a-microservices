-- Transactional outbox and the idempotent-receiver ledger.
--
-- No saga tables: this service takes part in no distributed flow. It emits
-- file-confirmed and file-deleted so other contexts can drop references to
-- files that no longer exist, and consumes nothing.

-- ===============
-- kafka_outbox (shared)
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
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_created_at ON kafka_outbox (created_at);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_topic ON kafka_outbox (topic);
CREATE INDEX IF NOT EXISTS idx_kafka_outbox_last_retry_at ON kafka_outbox (last_retry_at);

-- ===============
-- ===============
-- ===============
-- processed_event: the idempotent-receiver ledger.
--
-- Present even though this service consumes nothing today, because the outbox
-- dispatcher and the shared consumer plumbing expect it — and because a service
-- that starts consuming later should not need a migration to do so.
CREATE TABLE IF NOT EXISTS processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_processed_event_event_id_consumer UNIQUE (event_id, consumer_group)
);

CREATE INDEX IF NOT EXISTS idx_processed_event_processed_at ON processed_event (processed_at);
