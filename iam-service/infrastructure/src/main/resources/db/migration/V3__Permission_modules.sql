-- Every permission belongs to the module that enforces it. Services register their
-- own catalogue at start-up, so this column is written by them and never by hand;
-- it is what lets the administration panel group permissions without knowing in
-- advance which modules exist.
ALTER TABLE permission
    ADD COLUMN module VARCHAR(64) NOT NULL DEFAULT 'admin';

CREATE INDEX idx_permission_module ON permission (module);

-- A role that holds everything its modules offer, now and after they grow. Stored
-- as a flag rather than as a full set of rows, so a module shipped tomorrow is
-- covered the day it registers instead of when somebody remembers to tick it.
ALTER TABLE role
    ADD COLUMN unrestricted BOOLEAN NOT NULL DEFAULT FALSE;

-- Where a role can be held. A GLOBAL role is held platform-wide and lands in the
-- Keycloak token; a PROJECT role is held inside one project and is assigned by
-- project-service. Both draw from the same permission registry, so a module that
-- registers a permission can have it granted either way without knowing which.
ALTER TABLE role
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'GLOBAL';

CREATE INDEX idx_role_scope ON role (scope);
