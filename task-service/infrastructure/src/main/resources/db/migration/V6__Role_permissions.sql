-- What each role grants, as announced by iam-service. A local copy so an
-- authorization decision costs no call and survives iam being down; the role
-- name is the key because that is what a membership and a token both carry.
CREATE TABLE IF NOT EXISTS role_permission_projection (
    role_name VARCHAR(64) PRIMARY KEY,
    unrestricted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permission_projection_permission (
    role_name VARCHAR(64) NOT NULL REFERENCES role_permission_projection (role_name) ON DELETE CASCADE,
    permission VARCHAR(128) NOT NULL,

    PRIMARY KEY (role_name, permission)
);
