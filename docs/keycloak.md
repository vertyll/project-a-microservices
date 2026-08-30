# Keycloak Configuration

## How It Works

Keycloak is the identity provider (IdP) for the application. It handles:

- User credentials storage (passwords, enabled/disabled state).
- Token issuance (access tokens + refresh tokens, JWT format).
- Role management (mirrored from the app's IAM service).

> [!IMPORTANT]
>
> The realm JSON export: `keycloak/realm-config/realm-export.json` is automatically imported **on first startup** via
Docker Compose volume mount. You do **not** need to configure Keycloak manually.

## What the Realm Export Creates

| Resource                     | Name                   | Purpose                                                          |
|------------------------------|------------------------|------------------------------------------------------------------|
| Realm                        | `veds`                 | Application realm                                                |
| Realm roles                  | `USER`, `ADMIN`        | Mapped to Spring Security `ROLE_USER`, `ROLE_ADMIN`              |
| Client                       | `veds-api-gateway`     | Confidential client used by the Gateway BFF                      |
| Client                       | `veds-service-account` | Service account for IAM backend admin operations                 |
| Protocol mapper (predefined) | `roles mapper`         | Puts realm roles into `realm_access.roles` claim in access token |
| Protocol mapper (predefined) | `email mapper`         | Puts `email` claim in access token                               |

## Authentication Flow — Token Handler (BFF) with Authorization Code + PKCE

**No token of any kind reaches the browser.** The SPA holds one opaque HttpOnly cookie; the gateway keeps the access and
refresh tokens in Redis and injects `Authorization: Bearer`
on the way through to the microservices.

```text
Browser (SPA)              API Gateway (BFF)                Keycloak            Microservices
     |                            |                             |                     |
     |-- 1. GET /auth/authorize ->|                             |                     |
     |                            |-- 302, PKCE challenge ----->|                     |
     |<------------- Keycloak login page (user types password) -|                     |
     |                            |                             |                     |
     |-- 2. GET /auth/callback -->|                             |                     |
     |       ?code&state          |-- 3. code + verifier ------>|                     |
     |                            |<--- access + refresh token -|                     |
     |<-- 4. Set-Cookie: VEDS_SESSION (opaque id, HttpOnly, SameSite=Strict)          |
     |                            |    tokens stored in Redis   |                     |
     |                            |                             |                     |
     |-- 5. GET /projects ------->|                             |                     |
     |       + cookie             |-- 6. + Bearer <JWT> ------------------------------>|
     |                            |    (cookie swapped for token)                     |
```

1. **`GET /auth/authorize`** — gateway generates `state` and `code_verifier`, stores them in short-lived cookies,
   redirects to Keycloak with `code_challenge = S256(code_verifier)`.
2. **`GET /auth/callback`** — gateway verifies `state`, exchanges the code as a *confidential*
   client (client secret **and** PKCE verifier), and opens a server-side session.
3. **`GET /auth/session`** — the SPA's bootstrap call: "am I logged in, and as whom?". Returns id, e-mail and roles.
   Never a token.
4. **`POST /auth/logout`** — revokes the refresh token at Keycloak and deletes the session.

Each microservice still validates the JWT independently against Keycloak's JWKS endpoint — they are unaware a browser
session ever existed.

### Why the full Token Handler, not just PKCE plus a token in the response

Handing the SPA an access token — even with the refresh token safely in a cookie — leaves that access token in
JavaScript memory.

|                                           | Token in the SPA       | Token Handler                            |
|-------------------------------------------|------------------------|------------------------------------------|
| XSS can exfiltrate a usable token         | yes                    | no — only an HttpOnly cookie exists      |
| Token in JS memory / `localStorage` / URL | yes                    | no                                       |
| Revoking a session takes effect           | when the token expires | immediately — delete the Redis record    |
| SPA implements a refresh loop             | yes                    | no — the gateway refreshes transparently |

### Implementation notes

**Filter ordering.** The cookie→Bearer swap is a Spring `WebFilter` at `HIGHEST_PRECEDENCE`, *not* a Spring Cloud
Gateway `GlobalFilter`. Gateway filters run inside the routing handler, which is after Spring Security's chain — a token
injected there arrives too late and every request is rejected as anonymous.

**Session store: Redis, not an encrypted cookie.** Keycloak's two tokens exceed the 4 KB cookie budget once encrypted
and base64-encoded, refresh-token rotation would mean rewriting the cookie on every proxied request, and a server-side
record is what makes logout genuinely revoke access. It also lets any gateway replica serve any session.

**CSRF.** Once authentication travels in a cookie the browser attaches automatically, CSRF becomes a live concern that a
`Bearer` header did not have. `VEDS_SESSION` is `SameSite=Strict`, so it is never sent on a cross-site request. The two
login-flow cookies must be `Lax` — they are read on the callback, which *is* a cross-site redirect.

| Cookie                   | SameSite | Lifetime | Why                                                     |
|--------------------------|----------|----------|---------------------------------------------------------|
| `VEDS_SESSION`           | `Strict` | 7 days   | The only cookie the SPA relies on; CSRF defence         |
| `KEYCLOAK_AUTH_STATE`    | `Lax`    | 10 min   | Must survive the cross-site redirect back from Keycloak |
| `KEYCLOAK_CODE_VERIFIER` | `Lax`    | 10 min   | Read on the callback request                            |

> [!IMPORTANT]
>
> **`POST /auth/token` (ROPC) and `POST /auth/refresh-token` are gone.** ROPC is removed in
> OAuth 2.1 and required the gateway to receive the user's plaintext password, ruling out MFA,
> WebAuthn and identity brokering. `directAccessGrantsEnabled` is now `false` on the realm
> client. The refresh endpoint existed only to hand the browser a token — there is none.

### What lives where

| Concern                                               | Owner           |
|-------------------------------------------------------|-----------------|
| Passwords, sessions, MFA, token issuance, realm roles | **Keycloak**    |
| Browser session ↔ token mapping                       | **api-gateway** |
| Profile, terms consent, settings, role mirror         | **iam-service** |

Profile data is deliberately *not* stored in Keycloak user attributes: it is not an application database and querying it
is painful. The Admin API stays, in the narrower role of provisioning users at registration and syncing roles.

## Configuration

All Keycloak-related config is centralized in `shared-web/src/main/resources/shared-web-config.yml` and injected
into each service via `KeycloakProperties`.

## Where Do Role Names Live? (Microservices Anti–Shared-Kernel)

Role names are owned by **two places only**:

| Location           | Details                                                                                                                                                                                                                                                                                                        |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Keycloak realm** | (`keycloak/realm-config/realm-export.json`) — the runtime source of truth issued in every access token's `realm_access.roles` claim.                                                                                                                                                                           |
| **iam-service**    | (`iam-service/.../domain/model/RoleType.kt`, `internal`) — a type-safe mirror used solely by the role *administrator* (`RoleInitializer` seeds the DB, `AuthService.register` assigns `USER` via the Keycloak Admin API). The enum is `internal` to the iam-service module and intentionally **not** exported. |

> [!NOTE]
>
> Other microservices **do not** depend on iam-service's enum. They check roles as plain strings.

**Why no `shared-web.RoleType` enum:**

| Reason                        | Explanation                                                                                                                              |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| **Shared Kernel Antipattern** | (DDD antipattern, Evans, *DDD* ch. 14) — every change to the role vocabulary would force a coordinated recompile/deploy across services. |
| **Bounded Context Conflict**  | A role's *meaning* (what `ADMIN` is allowed to do) belongs to the service that owns the resource, not to a global enum.                  |
| **Source of Truth Drift**     | The IdP is already the source of truth; an in-code mirror would inevitably drift from Keycloak.                                          |

> [!NOTE]
>
> What stays in `shared-web/security/` is **only** the technical JWT → `Authentication` adapter
(`KeycloakJwtAuthenticationConverter` / `ReactiveKeycloakJwtAuthenticationConverter`). It is role-name-agnostic — it
maps *whatever* strings sit in the configured claim path onto `ROLE_*` authorities. Each service then decides which of
those it cares about, in its own `SecurityConfig`.

## Useful Keycloak URLs (Local Dev)

| URL                                                                | Description                             |
|--------------------------------------------------------------------|-----------------------------------------|
| http://localhost:9000                                              | Keycloak admin console                  |
| http://localhost:9000/realms/veds/.well-known/openid-configuration | OpenID Connect discovery                |
| http://localhost:9000/realms/veds/protocol/openid-connect/certs    | JWKS (public keys for JWT verification) |
| http://localhost:9000/realms/veds/protocol/openid-connect/token    | Token endpoint                          |

## Manual Token Request (curl)

```bash
# Get access token
curl -s -X POST http://localhost:9000/realms/veds/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=veds-api-gateway" \
  -d "client_secret=KEYCLOAK_GATEWAY_CLIENT_SECRET_HERE" \
  -d "username=test@example.com" \
  -d "password=Test1234!" | jq .
```

## Production TLS

`application-prod.yml` assumes TLS on every hop. Defaults are set for an ingress that terminates TLS in front of the
gateway; flip `SERVER_SSL_ENABLED` when a process holds the certificate itself.

| Leg                       | Setting                                                                                          |
|---------------------------|--------------------------------------------------------------------------------------------------|
| Browser → ingress/gateway | `SERVER_SSL_ENABLED`, `SERVER_SSL_BUNDLE`                                                        |
| Ingress → gateway         | `forward-headers-strategy: framework` (keeps the https scheme in redirect URIs and `Set-Cookie`) |
| Gateway → Redis           | `spring.data.redis.ssl.enabled=true`, `REDIS_SSL_BUNDLE`                                         |
| Service → PostgreSQL      | `DB_SSL_MODE=verify-full`, `DB_SSL_ROOT_CERT`                                                    |
| Service → Kafka           | `KAFKA_SECURITY_PROTOCOL=SASL_SSL`, SCRAM-SHA-512, truststore                                    |
| Cookie flag               | `veds.shared.keycloak.cookie.secure: true` (hardcoded, not overridable)                          |

Two choices worth stating:

- **`verify-full`, not `require`, for PostgreSQL.** `require` encrypts but accepts any certificate, so it stops passive
  sniffing and not an active man-in-the-middle.
- **`ssl.endpoint.identification.algorithm: https` for Kafka.** The default in some setups is empty, which disables
  hostname verification and reintroduces the same gap.

The `Secure` cookie flag is fixed to `true` in prod rather than read from an environment variable: a session cookie
without it can be sent over plain http and captured, and that is not a knob worth having.
