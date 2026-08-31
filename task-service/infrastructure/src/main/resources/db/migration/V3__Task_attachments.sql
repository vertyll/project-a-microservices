-- Files attached to a task itself, as opposed to a comment.
--
-- Ids only, and no foreign key: the file records live in file-service's
-- database. What keeps the two in step is the `file-deleted` event, which
-- removes the reference here rather than leaving a broken download behind.

CREATE TABLE IF NOT EXISTS task_attachment (
    task_id UUID NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL,
    CONSTRAINT uq_task_attachment UNIQUE (task_id, attachment_id)
);

-- Used when a file is deleted and every task referencing it has to be repaired.
CREATE INDEX IF NOT EXISTS idx_task_attachment_attachment ON task_attachment (attachment_id);
