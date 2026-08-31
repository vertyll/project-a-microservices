-- Task bounded context schema.
--
-- Identifiers are UUIDs assigned by the domain: an aggregate needs its id before
-- the transaction commits so outbox events can reference it, and the id crosses
-- service boundaries where a per-database sequence would not be unique.
--
-- There are deliberately no foreign keys to projects, categories, statuses or
-- users. Those rows live in other services' databases; what this schema holds
-- are *projections* of them, refreshed by integration events.

-- ===============
-- Aggregate root
CREATE TABLE IF NOT EXISTS task (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    description TEXT NOT NULL,
    additional_description TEXT,
    price_estimation INTEGER NOT NULL DEFAULT 0,
    worked_time INTEGER NOT NULL DEFAULT 0,
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status_id UUID,
    access_role_id UUID,
    created_by UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT chk_task_price_estimation CHECK (price_estimation >= 0),
    CONSTRAINT chk_task_worked_time CHECK (worked_time >= 0)
);

CREATE INDEX IF NOT EXISTS idx_task_status_id ON task (status_id);
CREATE INDEX IF NOT EXISTS idx_task_created_by ON task (created_by);
CREATE INDEX IF NOT EXISTS idx_task_is_active ON task (is_active);

-- The board is always filtered by project and almost always by active-only,
-- so the composite index is what the list query actually uses.
CREATE INDEX IF NOT EXISTS idx_task_board ON task (project_id, is_active, created_at DESC);

-- ===============
-- Element collections: ids owned elsewhere, so no foreign key to point at
CREATE TABLE IF NOT EXISTS task_category (
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    category_id UUID NOT NULL,
    CONSTRAINT uq_task_category UNIQUE (task_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_task_category_category ON task_category (category_id);

CREATE TABLE IF NOT EXISTS task_assignee (
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    CONSTRAINT uq_task_assignee UNIQUE (task_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_task_assignee_user ON task_assignee (user_id);

-- ===============
-- Comments
CREATE TABLE IF NOT EXISTS task_comment (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_task_comment_task ON task_comment (task_id);
CREATE INDEX IF NOT EXISTS idx_task_comment_author ON task_comment (author_id);

CREATE TABLE IF NOT EXISTS task_comment_attachment (
    comment_id UUID NOT NULL REFERENCES task_comment (id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL,
    CONSTRAINT uq_task_comment_attachment UNIQUE (comment_id, attachment_id)
);

-- ===============
-- Read models projected from project-service and iam-service events.
-- Never written by this service's own use cases.
CREATE TABLE IF NOT EXISTS project_ref (
    project_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS project_category_ref (
    category_id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    color VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_category_ref_project ON project_category_ref (project_id);

-- Every supported language arrives on the event, so the board never has to fall
-- back to another language when rendering a label.
CREATE TABLE IF NOT EXISTS project_category_ref_name (
    category_id UUID NOT NULL REFERENCES project_category_ref (category_id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_category_ref_name UNIQUE (category_id, language)
);

CREATE TABLE IF NOT EXISTS project_status_ref (
    status_id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    color VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_status_ref_project ON project_status_ref (project_id);

CREATE TABLE IF NOT EXISTS project_status_ref_name (
    status_id UUID NOT NULL REFERENCES project_status_ref (status_id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_status_ref_name UNIQUE (status_id, language)
);

-- Membership projection, read on every authorization decision.
CREATE TABLE IF NOT EXISTS project_membership_ref (
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_membership_ref_user ON project_membership_ref (user_id);

CREATE TABLE IF NOT EXISTS user_ref (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    avatar_file_id UUID,
    updated_at TIMESTAMP NOT NULL
);
