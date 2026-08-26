# Service Dependencies

What each service needs to build and to run, and what it does **not** need. The short answer:
no service calls another to serve a request, so any one of them can be down without taking the
others with it — but "autonomous" is not the same as "standalone", and the difference is below.

## Build-time

Composite-build dependencies, from each service's `settings.gradle.kts`.

| Service              | Shared libraries                                                  | Contract modules                                                                                   |
|----------------------|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| api-gateway          | `shared-infrastructure`                                           | —                                                                                                  |
| iam-service          | `shared-contracts`, `shared-infrastructure`                       | `iam-contracts`, `mail-contracts`                                                                  |
| mail-service         | `shared-contracts`, `shared-infrastructure`                       | `mail-contracts`, `iam-contracts`                                                                  |
| project-service      | `shared-contracts`, `shared-infrastructure`                       | `project-contracts`, `mail-contracts`, `iam-contracts`                                             |
| task-service         | `shared-contracts`, `shared-infrastructure`                       | `task-contracts`, `project-contracts`, `iam-contracts`, `file-contracts`                           |
| notification-service | `shared-contracts`, `shared-infrastructure`                       | `notification-contracts`, `project-contracts`, `task-contracts`, `mail-contracts`, `iam-contracts` |
| translation-service  | `shared-contracts`, `shared-infrastructure`, `shared-translation` | —                                                                                                  |
| file-service         | `shared-contracts`, `shared-infrastructure`                       | `file-contracts`                                                                                   |

A `-contracts` dependency means the service **reads or writes that context's events**, nothing
more: it pulls in generated Avro classes, not code. `task-service` depending on
`project-contracts` does not let it call project-service; it lets it deserialize the events
project-service publishes.

`shared-translation` is only in translation-service's build because the other services get it
transitively through `shared-infrastructure`, which exposes it with `api(...)`.

## Infrastructure at runtime

| Service              | Postgres               | Kafka + Schema Registry | Keycloak             | Other            |
|----------------------|------------------------|-------------------------|----------------------|------------------|
| api-gateway          | —                      | —                       | required             | Redis (sessions) |
| iam-service          | `iam_db`               | required                | required (admin API) | —                |
| mail-service         | `mail_service`         | required                | required (JWT)       | SMTP             |
| project-service      | `project_service`      | required                | required (JWT)       | —                |
| task-service         | `task_service`         | required                | required (JWT)       | —                |
| notification-service | `notification_service` | required                | required (JWT)       | —                |
| translation-service  | `translation_service`  | required                | required (JWT)       | —                |
| file-service         | `file_service`         | required                | required (JWT)       | Garage (S3 API)  |

Kafka is "required" everywhere because the outbox dispatcher and the saga engine are wired by
`shared-infrastructure` in every service. translation-service publishes nothing and consumes
nothing, so its outbox tables stay empty — they exist because the shared module's entities are
validated on start-up, and forking that module for one service would cost more than four empty
tables.

## Service-to-service

**No service calls another to serve a request.** There is exactly one synchronous call in the
system, and it is not on a request path:

| Caller        | Callee              | When                                              | If it fails                       |
|---------------|---------------------|---------------------------------------------------|-----------------------------------|
| every service | translation-service | once at start-up, to publish its translation keys | logged; the service starts anyway |

Everything else travels through Kafka. See the [Event Catalogue](./events.md) for who publishes
and who consumes what.

## What that buys, and what it costs

A service keeps working when the ones it hears from are down, because it holds its own
projection of what it needs — `task-service` renders a board with project names and assignee
names without asking anybody.

The cost is stated rather than hidden: those projections are **eventually consistent**. A member
removed from a project a second ago may still pass an authorization check in task-service until
the event arrives. The alternative — asking project-service on every task read — would couple
the availability of the two services, which is the thing this design is spending consistency to
avoid.

## Start-up order

Only one ordering matters, and it is a convenience rather than a requirement: starting
translation-service first avoids a failed registration in the other services' logs. Registration
failure is deliberately non-fatal and the keys are republished on the next restart.

Everything else is `depends_on` in `docker-compose.yml`, which waits on health checks.
