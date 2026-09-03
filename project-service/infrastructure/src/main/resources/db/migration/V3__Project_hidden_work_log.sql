-- A project may keep hidden work log entries alongside the normal ones. The role
-- codes listed here are the only ones allowed to read them, besides each entry's
-- own author; the list stays empty while the feature is off.
ALTER TABLE project
    ADD COLUMN hidden_work_log_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE project_hidden_work_log_role
(
    project_id UUID        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    role_code  VARCHAR(32) NOT NULL,

    PRIMARY KEY (project_id, role_code)
);
