# iam-service

Users, roles, permissions and the account lifecycle.

|           |                                                             |
|-----------|-------------------------------------------------------------|
| Port      | 8082                                                        |
| Database  | 5433                                                        |
| Publishes | `user-registered`, `user-profile-updated`, `mail-requested` |
| Consumes  | `mail-sent`, `mail-failed`                                  |

## Permissions belong to roles

They used to hang off users, which is not RBAC: granting access became a list of tick boxes per
person, and "what can a manager do" had no answer. `V4__Role_permission_mapping.sql` moves them
and drops the user table.

Per-user exceptions were **not** kept. Two sources of truth mean an audit has to consult both,
and the administration screen could only ever show half the picture. `User.permissions` is
derived from roles and never stored.

## Credentials

Keycloak is the source of truth for authentication; this service owns the profile and the
authorization model. Password hashes are not stored here.

"No such user" and "wrong password" report the **same** error, `iam.auth.invalid_credentials`.
Telling them apart is a user-enumeration oracle.

## Registration is a saga

Creating a Keycloak identity, storing the user and sending the activation mail cannot be one
transaction. The saga compensates: a failed mail delivery rolls the account back rather than
leaving one nobody can activate. See [Sagas and Outbox](../docs/saga-and-outbox.md).

## Second factor

TOTP is optional and configured on Keycloak's own pages, reached through the gateway with
`kc_action=CONFIGURE_TOTP`. **No TOTP secret ever passes through this service, its logs or the
browser's JavaScript** — there is no second implementation of a security-critical flow to keep
correct.

`GET /auth/me/security` reports the status by asking Keycloak which credentials the user holds.
It is not stored here: a copy would be a second answer to the same question, wrong the moment
somebody configures a factor on Keycloak's pages directly.

Disabling is a normal API call, because it has no secret to handle. The gateway allows only a
fixed set of `kc_action` values — an open one would let a caller push a user into any Keycloak
flow, including ones that change credentials.

## One known mis-signal

`validatePassword` returns `false` for any exception from Keycloak, so an unreachable Keycloak
is reported to the user as a wrong password. It is used when confirming a password change, not
when signing in — sign-in happens on Keycloak's own pages — so the blast radius is small, but
the message is misleading and the cause is invisible in the response.

Fixing it properly means distinguishing "rejected" from "unavailable" in the port's return type.
Left as is deliberately rather than silently: it is a known wart, not an oversight.
