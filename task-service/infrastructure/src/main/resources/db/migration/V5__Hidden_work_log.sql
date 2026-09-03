-- A hidden entry is readable by its author and by the project roles listed in the
-- project projection; everyone else never sees it. Entries are visible by default.
ALTER TABLE task_work_log
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE project_ref
    ADD COLUMN hidden_work_log_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE project_ref_hidden_work_log_role
(
    project_id UUID        NOT NULL REFERENCES project_ref (project_id) ON DELETE CASCADE,
    role_code  VARCHAR(32) NOT NULL,

    PRIMARY KEY (project_id, role_code)
);
