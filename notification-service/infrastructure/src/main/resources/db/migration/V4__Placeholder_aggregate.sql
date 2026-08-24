-- Placeholder aggregate table for notification-service.
--
-- The scaffolding maps NotificationJpaEntity to the "notification" table, but template-service ships no
-- migration for it, so `ddl-auto: validate` fails on a fresh database. This migration
-- makes the generated service boot as-is.

CREATE TABLE IF NOT EXISTS notification (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_status ON notification (status);
