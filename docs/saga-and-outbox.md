# Saga Pattern & Transactional Outbox

## Overview

The system uses **choreography-based sagas** — there is no central orchestrator. Each participating service runs its own
local saga and progresses by reacting to domain events on Kafka.

---

## Polyglot Persistence via Shared Contracts

Both the **Saga** and **Transactional Outbox** patterns are built on top of database-agnostic ports — the saga ports in
`shared-saga-engine`, the outbox ports in `shared-messaging-kafka`. To introduce a different storage (MongoDB,
PostgreSQL, …), you only implement the ports against the new technology; the engines (which ship JPA flavors like
`BaseSaga`, `BaseSagaStep`, `BaseOutbox`, etc.) do not change.

| Contract                                            | Purpose                                                                                                                        |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `Saga<S : Saga<S>>`<br/>`SagaStep<T : SagaStep<T>>` | Rich-aggregate ports with F-bounded generics — behavior methods return the concrete adapter type, eliminating unchecked casts. |
| `SagaRepositoryPort`<br/>`SagaStepRepositoryPort`   | Persistence ports for sagas.                                                                                                   |
| `OutboxMessage`<br/>`OutboxRepositoryPort`          | Outbox aggregate + repository port (with `lockBatchForDispatch` for `SELECT … FOR UPDATE SKIP LOCKED`).                        |
| `ProcessedEventRepositoryPort`                      | Idempotent-receiver ledger (UNIQUE `(eventId, consumerGroup)`).                                                                |

---

## Canonical Building Blocks

All building blocks live in `shared-messaging-kafka` and `shared-saga-engine`, and provide the foundation for robust asynchronous processing.

### Transactional Outbox

Consists of `KafkaOutboxProcessor` (poller) and `OutboxDispatchTx` (transactional helper).

- **Two-phase dispatch** — Claims a batch with `SELECT … FOR UPDATE SKIP LOCKED` (`READY → PROCESSING`). Kafka
  publication happens *outside* any DB transaction. Success/failure is recorded in a fresh `REQUIRES_NEW` transaction
  (`COMPLETED`, `markRetryScheduled`, `markDeadLettered`).
- **Idempotency** — UNIQUE constraint on `event_id`.
- **Reaper** — Rescues stuck `PROCESSING` rows abandoned by a crash.
- **Config** — Externalized via `KafkaOutboxProperties`.

### Idempotent Receiver

`ProcessedEventGuard.claim(eventId, consumerGroup)` writes to a ledger with a UNIQUE constraint on
`(eventId, consumerGroup)`. A delivery whose id is already there is skipped.

The claim is written **in the handler's own transaction**, so the two share a fate. That is what makes the
retry and dead-letter machinery mean anything:

| Handler outcome | Claim                              | Redelivery                                                  |
|-----------------|------------------------------------|-------------------------------------------------------------|
| commits         | committed with the business writes | skipped as a duplicate                                      |
| throws          | rolled back with them              | a real retry, and after `MAX_RETRIES` the dead letter topic |

A claim committed independently would invert the second row: every retry would find its own claim, report a
duplicate and return successfully, so a transient failure would drop the message on its first attempt and
nothing would ever reach the dead letter topic.

Delivery is therefore **at-least-once**, and every `@KafkaListener` that claims is `@Transactional` so the
claim and the handler share one unit of work. A handler must tolerate being re-entered after a failure.

Two consumers racing on the same event both pass the existence check; one loses on the UNIQUE constraint and
rolls back, and its redelivery then sees the committed row and skips.

### Saga Engine

Generic `SagaEngine<S, T>` with choreography semantics. On step failure or `failSaga`, an *after-commit* hook
(`TransactionSynchronizationManager`) delegates to `SagaCompensationRunner.runCompensation` in a `REQUIRES_NEW`
transaction (runs only if business tx commits).

### Saga Watchdog

A scheduled job (`@Scheduled`) that times out sagas stuck in `AWAITING_RESPONSE` and retries compensation for
`COMPENSATING` / `COMPENSATION_FAILED` states based on a cooldown.

### Compensation Topic

Follows the convention `SagaCompensationTopic.PREFIX + "<service>"` — each service composes its own neutral topic (e.g.
`saga-compensation-iam`).

### Saga Log Correlation

Feedback events carry `sagaId`; the originating saga sits in `AWAITING_RESPONSE` until matched.

### Recovery Jobs

Service-local `SchedulingConfig` wires `@EnableScheduling` so `KafkaOutboxProcessor` and `SagaWatchdog` ticks fire.

---

## Example: User Registration Flow

This example illustrates the choreography between `iam-service` and `mail-service`.

> [!IMPORTANT]
>
> Compensation only exists where effects are reversible. A sent email cannot be un-sent, therefore `mail-service` does
**not** have a `SagaCompensationService`.

### Phase 1 — Init (`iam-service`)

Begins local saga `USER_REGISTRATION`. Records steps:

1. `CREATE_USER`
2. `PUBLISH_USER_REGISTERED_EVENT`
3. `CREATE_VERIFICATION_TOKEN`
4. `PUBLISH_MAIL_REQUESTED_EVENT`

Transitions to `AWAITING_RESPONSE`. Inserts `MailRequestedEvent` into the outbox (same JDBC tx, no dual-write).

### Phase 2 — Publish (Outbox Poller)

Publishes the Avro-serialized `MailRequestedEvent` to Kafka.

### Phase 3 — Process (`mail-service`)

Consumes event (`MailEventConsumer`), claims via `ProcessedEventGuard`. Begins local saga `EMAIL_SENDING`, performs
`SEND_EMAIL`, completes saga. Inserts `MailSentEvent` (or `MailFailedEvent`) into its outbox.

### Phase 4 — Feedback (`iam-service`)

Consumes feedback via `MailFeedbackConsumer`.

- **On `MailSentEvent`** — Calls `markSagaCompleted`.
- **On `MailFailedEvent`** — Calls `failSaga` → after-commit hook fires `SagaCompensationRunner` → `IamSagaCompensator`
  publishes to `saga-compensation-iam` → `AuthCompensationService` rolls back Keycloak user, token, etc.

---

## Event-Driven Communication

Services communicate asynchronously through Kafka events. Integration events are defined as **Avro** schemas under
`contracts/<service>/<topic>/v<n>/*.avsc` and serialized in binary form with Schema Registry.

> [!NOTE]
>
> All publishing goes through the Outbox (`KafkaOutboxProcessor`); all consumption goes through `ProcessedEventGuard`
for idempotency.

| Event                                 | Publisher      | Consumer       | Details                                                                                                                                                                                                                                                                                                                                               |
|---------------------------------------|----------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MailRequestedEvent`                  | `iam-service`  | `mail-service` | Published via `AuthEventPublisherPort` / `KafkaAuthEventPublisherAdapter`. Consumed by `MailEventConsumer`.                                                                                                                                                                                                                                           |
| `MailSentEvent`<br/>`MailFailedEvent` | `mail-service` | `iam-service`  | Published through the Transactional Outbox. Consumed by `MailFeedbackConsumer` to advance or fail the originating saga.                                                                                                                                                                                                                               |
| Compensation Actions                  | `iam-service`  | `iam-service`  | Published to internal `saga-compensation-iam` topic as an Avro **tagged union** (`DeleteUserAction`, `DeleteVerificationTokenAction`, etc.). Decoded by `AvroAuthCompensationCommandTranslator` (ACL) into a typed `sealed interface AuthCompensationCommand`. Handled via compile-time exhaustive `when` (no stringly-typed discriminator or `Map`). |
