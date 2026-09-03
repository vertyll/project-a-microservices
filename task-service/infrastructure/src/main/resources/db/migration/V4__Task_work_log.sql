-- Individual work log entries. task.worked_time holds their sum in minutes;
-- it is no longer typed in by hand, so the two can never disagree.
ALTER TABLE task RENAME COLUMN worked_time TO worked_minutes;
ALTER TABLE task RENAME CONSTRAINT chk_task_worked_time TO chk_task_worked_minutes;
UPDATE task SET worked_minutes = round(worked_minutes * 0.6);

CREATE TABLE task_work_log
(
    id          UUID PRIMARY KEY,
    task_id     UUID        NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id   UUID        NOT NULL,
    minutes     INTEGER     NOT NULL,
    worked_on   DATE        NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT chk_work_log_minutes CHECK (minutes > 0 AND minutes <= 1440)
);

CREATE INDEX idx_task_work_log_task_id ON task_work_log (task_id, worked_on);
CREATE INDEX idx_task_work_log_author_id ON task_work_log (author_id);
