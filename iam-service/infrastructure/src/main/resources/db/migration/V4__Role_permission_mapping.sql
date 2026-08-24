-- Permissions move from users to roles.
--
-- Attaching permissions directly to users is not RBAC: granting access becomes a
-- list of tick boxes per person, and "what can a manager do" has no answer.
-- project-service already modelled this correctly (ProjectRole owns its
-- permissions); IAM was the outlier.
--
-- This also matters for what comes next. With several organisations, per-user
-- permissions are unauditable — nobody can say which people hold a given right
-- or why.

CREATE TABLE IF NOT EXISTS role_permission_mapping (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_mapping_role
        FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_mapping_permission
        FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_role_permission_mapping_permission
    ON role_permission_mapping (permission_id);

-- Carry existing grants over before dropping the old table: every permission a
-- user held becomes a permission of every role they had. Coarser than what was
-- there, deliberately — the alternative is inventing a role per combination.
INSERT INTO role_permission_mapping (role_id, permission_id)
SELECT DISTINCT urm.role_id, upm.permission_id
FROM user_permission_mapping upm
JOIN user_role_mapping urm ON urm.user_id = upm.user_id
ON CONFLICT DO NOTHING;

-- Dropped rather than kept as per-user exceptions. Exceptions are the loophole
-- through which RBAC dissolves: with two sources of truth, an audit has to
-- consult both and the panel can only ever show half the picture.
DROP TABLE IF EXISTS user_permission_mapping;
