-- Where a permission can be held. Enforced against a project membership or against
-- the platform, never both: granting a project role the right to edit users, or a
-- platform role the right to comment on a task nobody assigned them, is not a
-- decision an administrator should be able to make by accident.
ALTER TABLE permission
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'PROJECT';
