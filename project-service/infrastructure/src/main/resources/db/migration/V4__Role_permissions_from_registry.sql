-- What a project role grants is decided in iam-service and arrives on
-- role-permissions-changed. The column widens because a role may now hold a
-- permission another module declares, and the flag covers the role that holds
-- everything, including permissions no module has registered yet.
ALTER TABLE project_role_permission
    ALTER COLUMN permission TYPE VARCHAR(128);

ALTER TABLE project_role
    ADD COLUMN unrestricted BOOLEAN NOT NULL DEFAULT FALSE;

-- Seeing tasks is the task module's decision; the two names meant one thing.
UPDATE project_role_permission SET permission = 'VIEW_TASKS' WHERE permission = 'SHOW_TASKS';

-- A role code is no longer one of three: an administrator creates project-scoped
-- roles in iam and this context learns them from the same event.
ALTER TABLE project_role
    ALTER COLUMN code TYPE VARCHAR(64);
