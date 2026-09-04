-- What each role grants, as announced by iam-service. Reading the delivery log is
-- guarded by a permission, and the answer has to be local so mail keeps working
-- while iam is down.
CREATE TABLE role_permission_projection (
    role_name VARCHAR(64) PRIMARY KEY,
    unrestricted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE role_permission_projection_permission (
    role_name VARCHAR(64) NOT NULL REFERENCES role_permission_projection (role_name) ON DELETE CASCADE,
    permission VARCHAR(128) NOT NULL,

    PRIMARY KEY (role_name, permission)
);
