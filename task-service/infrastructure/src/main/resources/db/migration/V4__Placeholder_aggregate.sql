-- Placeholder aggregate table for task-service.
--
-- The scaffolding maps TaskJpaEntity to the "task" table, but template-service ships no
-- migration for it, so `ddl-auto: validate` fails on a fresh database. This migration
-- makes the generated service boot as-is.

CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_status ON task (status);
