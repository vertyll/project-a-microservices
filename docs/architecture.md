# Architecture

## Overview

A microservices-based architecture following principles:

- Domain-Driven Design.
- Event-Driven Architecture.
- Hexagonal Architecture.
- Separation of Concerns.
- Choreography pattern for service coordination.
- Saga pattern for distributed transactions.
- Outbox pattern for reliable event publishing.
- SOLID.

The project is split into the following components:

1. **API Gateway** – Entry point for all client requests, handles routing to appropriate services, JWT validation, and
   BFF (Backend-For-Frontend) auth proxy to Keycloak.
2. **IAM Service** – Consolidates user management, roles, permissions, and account operations. Authentication is
   delegated to Keycloak.
3. **Mail Service** – Handles email sending operations and templates.
4. **Shared Infrastructure** – Engines, contracts, and utilities used across all microservices.
5. **Each `*-contracts`** – Per-bounded-context Avro Published Language modules. Each holds only Avro schemas and the
   Java `SpecificRecord` classes generated from them.
6. **Project Service** – Owns projects, project types, categories, statuses, project roles and memberships.
7. **Task Service** – Owns tasks, comments and the board.
8. **Notification Service** – Owns in-app notifications, delivery settings and the STOMP push transport.
9. **Translation Service** – Owns the translation catalogue: keys, languages and their text.
10. **File Service** – Owns file metadata and issues pre-signed URLs; the bytes live in object storage.
11. **Template Service** – Baseline configuration for future microservices.

Each service documents its own decisions in its `README.md`; this page covers what is true across all of them.

### Where the reasoning lives

| Kind                      | Where                                 |
|---------------------------|---------------------------------------|
| Cross-cutting decisions   | `docs/`                               |
| A service's own decisions | that service's `README.md`            |
| Library API contracts     | KDoc, published with `./gradlew docs` |

| Where                                   | What a reasonable edit would break                                                             |
|-----------------------------------------|------------------------------------------------------------------------------------------------|
| `SessionTokenRelayFilter.getOrder`      | Converting it to a Gateway `GlobalFilter` rejects every request as anonymous                   |
| `ProjectAccessPolicy.RULES`             | Reordering lets an owner edit an archived project                                              |
| `TaskAccessPolicy.ROLE_PERMISSIONS`     | Adding a default for unknown codes grants new project roles task permissions nobody decided on |
| `Task.moveTo`                           | "Fixing" the `this` return makes every board drag notify every watcher                         |
| `TranslationValue.withSeededDefault`    | Merging the two columns makes each redeploy revert administrators' edits                       |
| `IamError.INVALID_CREDENTIALS`          | Splitting it into two codes is a user-enumeration oracle                                       |
| `FileCommandService.delete`             | Deleting the object first loses the key on a rollback                                          |
| `ObjectStorageConfig`                   | Removing path-style access breaks against Garage                                               |
| `OutboxJpaRepository` lock hint         | Dropping `lock.timeout = -2` loses `SKIP LOCKED`, so outbox dispatchers block on each other    |
| `IdentityProviderPort.removeCredential` | Allowing a password to be removed locks the account out with no way back                       |
| `MailFeedbackService` saga types        | Completing them on mail delivery skips the user confirmation the saga is waiting for           |
| `KeycloakIdentityProviderAdapter`       | Parsing a missing `Location` header yields a UUID error naming neither cause nor account       |

Anything that is merely *interesting* goes in a README instead.

### Shared modules

| Module                      | Contains                                                                              | Depends on                       |
|-----------------------------|---------------------------------------------------------------------------------------|----------------------------------|
| `shared-saga-api`           | Saga vocabulary (`Saga`, `SagaStep`, `SagaStatus`, `SagaStepStatus`, `SagaTypeValue`) | Kotlin stdlib only               |
| `shared-translation`        | Translation key DSL, ICU renderer, pattern validation                                 | ICU4J                            |
| `shared-web`                | Keycloak JWT converters, ETag and optimistic-locking helpers                          | Spring Security                  |
| `shared-messaging-kafka`    | Outbox, idempotent consumption, Avro serialisation                                    | Spring, Kafka, JPA               |
| `shared-saga-engine`        | Saga orchestration, compensation, watchdog, JPA base entities                         | `shared-saga-api`, Spring, JPA   |
| `shared-translation-client` | Start-up registration of a service's translation keys                                 | `shared-translation`, Spring Web |

The framework-free modules exist so that a service's **application layer can reference saga statuses without putting
Spring on its compile classpath**. The Spring-bound modules are split by capability, and only where some consumer takes
one without the others: the gateway takes `shared-web` alone, file-service takes the outbox without the saga engine.

[Shared Modules](./shared-modules.md) is the canonical reference for what each one holds and why.

> [!NOTE]
>
> Each microservice follows Hexagonal Architecture principles with a three-layer structure and has its own PostgreSQL
database. Services communicate with each other
> asynchronously via Apache Kafka (event-driven, choreography-based), with the Transactional Outbox pattern guaranteeing
reliable event publication.

## Components

### Kafka KRaft Mode

The system uses Kafka in KRaft mode (Kafka Raft), eliminating the need for Zookeeper.

| Aspect            | Implementation Details                                                                                        |
|-------------------|---------------------------------------------------------------------------------------------------------------|
| **Configuration** | Configuration is handled via `KAFKA_PROCESS_ROLES` (broker, controller) and `KAFKA_CONTROLLER_QUORUM_VOTERS`. |
| **Setup**         | A static `CLUSTER_ID` is provided in `docker-compose.yml` for simplified setup.                               |

### Shared Infrastructure

Provides shared engines, event definitions, and utilities for all microservices.

| Component                 | Description                                                                                                                                                                                        |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Persistence Contracts** | Implements persistence-agnostic contracts for the Transactional Outbox and Saga patterns (`OutboxRepositoryPort`, `ProcessedEventRepositoryPort`, `SagaRepositoryPort`, `SagaStepRepositoryPort`). |
| **Kafka Blocks**          | Reusable Kafka building blocks: `KafkaOutboxProcessor` + `OutboxDispatchTx`, `ProcessedEventGuard`, `AvroPayloadSerializer`/`AvroPayloadDeserializer`.                                             |
| **IdP Adapter**           | Shared technical IdP adapter for OAuth2/OIDC that translates configured roles into Spring Security authorities without knowing concrete role names.                                                |
| **Saga Engine**           | Generic `SagaEngine`, `SagaCompensationRunner`, and `SagaWatchdog` operating on F-bounded contracts with zero unchecked casts.                                                                     |
| **Configuration**         | Externalized configuration (`KafkaOutboxProperties`, `SagaProperties`) auto-registered by `OutboxAndSagaAutoConfiguration`.                                                                        |
| **Composable Contracts**  | Inter-service conventions, such as `SagaCompensationTopic.PREFIX` for defining participant topics.                                                                                                 |
| **Event Schemas**         | Integration event schemas defined as Avro binary wire format with Schema Registry.                                                                                                                 |

### Avro Published Language Modules

| Aspect                          | Details                                                                                                                                     |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| **Separate Module Rationale**   | Lifting contracts into composite-build modules ships them as plain library JARs, avoiding Spring Boot DevTools `ClassCastException` issues. |
| **Module Contents**             | A single `java-library` Gradle build that runs `avro-tools` over `*.avsc` files to produce a `SpecificRecord` JAR.                          |
| **Schema Location**             | IAM/Mail read schemas from the repository-root `contracts/` directory; Template service keeps them locally in `template-contracts/avro/`.   |
| **Anti-Corruption Layer (ACL)** | Generated Avro types never leave the infrastructure layer and are decoded into a typed Kotlin `sealed interface` for the application layer. |
| **DevTools**                    | Intentionally not declared in service infrastructure layers to prevent classloader conflicts.                                               |

### IAM Service

Responsible for user profile management, authorization, and account operations.

| Feature               | Details                                                                                                                      |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------|
| **Authentication**    | Fully delegated to Keycloak via the `IdentityProviderPort` outbound port.                                                    |
| **Admin Integration** | Uses Keycloak Admin API via a service account for user provisioning and role sync.                                           |
| **Database Stores**   | Stores user profile data, role definitions, permissions, verification tokens, and local saga state for distributed flows.    |
| **Endpoints**         | Provides endpoints for registration, profile management, and role administration.                                            |
| **Outbound Events**   | Publishes events (e.g., `MailRequestedEvent`) to Kafka through the `AuthEventPublisherPort` outbound port.                   |
| **Saga Compensation** | Consumes mail feedback events and runs compensation logic via `AuthCompensationService` based on decoded Avro tagged unions. |

### Mail Service

Responsible for sending emails based on Thymeleaf templates.

| Feature                 | Details                                                                                                           |
|-------------------------|-------------------------------------------------------------------------------------------------------------------|
| **Database Stores**     | Stores email logs, delivery status, and local saga state.                                                         |
| **Inbound Events**      | Consumes `MailRequestedEvent` events from other services via `MailEventConsumer`.                                 |
| **Outbound Events**     | Publishes `MailSentEvent` or `MailFailedEvent` back through the Transactional Outbox.                             |
| **Templates Supported** | Supports various email templates like welcome, password reset, account activation, and email change confirmation. |
| **Architecture Role**   | Acts as a reactive choreography participant that decouples email sending logic from the SMTP provider.            |

### Template Service

A baseline skeleton for spinning up a new microservice.

| Feature                   | Details                                                                                                                            |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| **Schema Isolation**      | Excluded from the global `contracts/` directory; schemas live locally so they are not picked up by production provisioners.        |
| **Package Layout**        | Provides a Hexagonal package layout containing domain, application, and infrastructure layers.                                     |
| **Domain & Outbox**       | Includes a placeholder domain and Transactional Outbox scaffolding implementing the shared read-only `OutboxMessage` contract.     |
| **Saga Scaffolding**      | Provides local saga model, `SagaProcessPort`, and an adapter delegating to the shared `SagaEngine`.                                |
| **Compensation Pipeline** | Features a typed compensation pipeline mirroring IAM, including an Anti-Corruption Layer and serializers.                          |
| **Kafka Integration**     | Contains a Kafka skeleton with an outbound publisher adapter and an inbound consumer.                                              |
| **Web Skeleton**          | Provides a REST controller (`/template`) and corresponding DTOs.                                                                   |
| **Cloning Instructions**  | Requires copying service and contracts, renaming packages, moving Avro schemas to the shared directory, and updating Gradle paths. |

## Design Principles

### Hexagonal Architecture (Ports & Adapters)

Benefits:

- **Testability**: Core business logic can be tested independently.
- **Flexibility**: Easy to swap external dependencies without affecting business logic.
- **Maintainability**: Clear separation between business rules and technical details.
- **Technology Independence**: Core domain is not coupled to specific frameworks or technologies.

### Domain-Driven Design (DDD)

Each microservice is designed around a specific business domain with:

- A clear bounded context.
- Domain models that represent business entities.
- A layered architecture within the hexagonal structure.
- Domain-specific language.
- Encapsulated business logic.

### Event-Driven Architecture

- Services communicate asynchronously via events (Kafka).
- Promotes loose coupling and high scalability.
- Enables reactive, responsive systems.
- Aligns with the choreography pattern for service coordination.
- Supports eventual consistency and distributed transactions via the Saga pattern.
- Supports the Outbox pattern for reliable event publication.

### SOLID Principles and Separation of Concerns

The codebase adheres to:

- **Single Responsibility Principle**: Each class has a single responsibility.
- **Open/Closed Principle**: Classes are open for extension but closed for modification.
- **Liskov Substitution Principle**: Subtypes are substitutable for their base types.
- **Interface Segregation Principle**: Specific interfaces rather than general ones.
- **Dependency Inversion Principle**: Depends on abstractions (ports), not concretions (adapters).

> [!NOTE]
>
> The hexagonal architecture naturally enforces these principles by isolating business logic from external concerns,
> using dependency inversion through ports and adapters, and maintaining clear boundaries between layers.

### Choreography Pattern for Service Coordination

The system uses a choreography-based approach for service coordination:

- Services react to events published by other services without central coordination.
- Each service knows which events to listen for and what actions to take.
- No central orchestrator is needed, making the system more decentralized and resilient.
- Services maintain autonomy and can evolve independently.

Benefits:

- Reduced coupling.
- Flexible and scalable architecture.
- Easier to add/modify services.
- Better resilience (no single point of failure).
- Natural alignment with hexagonal architecture.

## Two levels of authorization

They are different mechanisms and must not be mixed.

| Level    | Question it answers                         | Where                                     |
|----------|---------------------------------------------|-------------------------------------------|
| Platform | may this person administer the application? | realm roles + `Permission` in iam-service |
| Project  | may this person edit *this* project?        | `ProjectRole` + `ProjectAccessPolicy`     |

The project level is attribute-based: `ProjectAccessPolicy` reads attributes of the resource (`isPublic`, `isActive`),
not only the subject's role, which is why "nobody may edit an archived project, not even its owner" is expressible
there.

The platform level is plain RBAC, deliberately. There is no resource attribute an administration decision could depend
on — editing translations is not something one can be allowed to do inside one project and not another.

### Permissions belong to roles

A permission is granted to a role, never to a person. Attaching permissions to users directly is not RBAC: granting
access becomes a list of tick boxes per person, and "what can a manager do" has no answer. project-service models
project roles the same way.

There are **no** per-user exceptions, by design: two sources of truth mean an audit has to consult both, and the
administration screen could only ever show half the picture. `User.permissions` is derived from the user's roles at
read time and never stored, so a grant cannot drift from the role that justifies it.

Credentials and sessions belong to Keycloak, not here. The schema holds a profile joined to Keycloak by `keycloak_id`,
and carries no password column and no refresh-token table — see [Keycloak](./keycloak.md).

This also matters for what comes next. With several organizations, per-user permissions are unauditable — nobody could
say who holds a given right, or why. The organization level, when it arrives, is a copy of the project pattern one floor
up: a role scoped to the organization, plus a policy object. The decision already flows through a policy object, so
adding an organization attribute is one file rather than a review of every use case.
