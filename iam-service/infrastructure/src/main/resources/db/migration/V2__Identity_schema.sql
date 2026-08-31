-- Identity: users, roles and the permissions a role grants.
--
-- Keycloak owns credentials and sessions. What is here is the profile and the authorization
-- model, joined to Keycloak by `keycloak_id`; there is no password column and no refresh token,
-- because neither is this service's to hold.
--
-- A permission is granted to a role, never to a person. Attaching permissions directly to users
-- is not RBAC: granting access becomes a list of tick boxes per person, "what can a manager do"
-- has no answer, and nobody can say which people hold a given right. `User.permissions` is
-- derived from roles at read time and never stored — see docs/architecture.md.

-- ===============
-- The person: profile only, joined to Keycloak by keycloak_id
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,
    keycloak_id UUID NULL,
    avatar_file_id UUID NULL,

    CONSTRAINT uk_user_email UNIQUE (email)
);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON "user" (created_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_keycloak_id ON "user" (keycloak_id);

-- ===============
-- Authorization: roles, the permissions they grant, and who holds them
CREATE TABLE IF NOT EXISTS role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_role_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS permission (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_permission_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_role_mapping (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_mapping_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_mapping_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_permission_mapping (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_mapping_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_mapping_permission FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_role_permission_mapping_permission ON role_permission_mapping (permission_id);

-- ===============
-- Single-use tokens for activation, e-mail change and password reset
CREATE TABLE IF NOT EXISTS verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    token_type VARCHAR(255) NOT NULL,
    additional_data TEXT NULL,
    saga_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NULL,

    CONSTRAINT uk_verification_token_token UNIQUE (token)
);
CREATE INDEX IF NOT EXISTS idx_verification_token_username ON verification_token (username);
