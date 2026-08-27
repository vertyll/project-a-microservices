# Hexagonal Layering

How the dependency rule is enforced in practice, and what it cost.

> [!NOTE]
>
> Applied in **project-service**, **iam-service**, **mail-service** and
> **template-service** — see [Status](#status).

## The rule

Dependencies point inwards only:

```
infrastructure ──► application ──► domain
```

| Layer            | May depend on                | Contains                                                   |
|------------------|------------------------------|------------------------------------------------------------|
| `domain`         | Kotlin stdlib                | Aggregates, value objects, repository *ports*, policies    |
| `application`    | `domain`, `shared-saga-api`  | Use cases, commands, response DTOs, inbound/outbound ports |
| `infrastructure` | everything                   | JPA, Kafka, Avro, web, Spring wiring, adapters             |

`shared-saga-api` is a module holding only the saga vocabulary, with the Kotlin standard library as its single
dependency. Those types used to sit in the Spring-bound shared module, so importing a saga status pulled the whole
framework onto the application classpath.

In project-service the application layer's entire external import surface is:

```
com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue
com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
java.time.Instant
java.util.UUID
```

No Spring, no `jakarta.validation`, no Jackson, no SLF4J.

## What that required

### `@Service` → explicit beans

Component scanning is replaced by `infrastructure/config/ApplicationBeansConfig`, which constructs each use case by
hand. Verbose, but it also means every use case is constructible in a plain unit test with no Spring context.

### `@Transactional` → a decorator at the port

The transaction *boundary* is a use case; the transaction *mechanism* is infrastructure.
`TransactionalUseCaseFactory` wraps each inbound port in a dynamic proxy that runs every call inside a
`TransactionTemplate`, with the read-only methods named per port.

A dynamic proxy rather than seven handwritten decorators: those would be several hundred lines of pure delegation, and
each new use-case method would need a matching edit or would silently run without a transaction. The trade-off is that
read-only method names are checked at startup rather than by the compiler — a typo fails the context, it does not
degrade quietly.

> An earlier attempt inlined `transactionRunner.inTransaction { … }` into each method body.
> It does not work: a `return` inside a non-inline lambda is a non-local return and will not
> compile. The decorator avoids touching method bodies at all.

### Transaction mode follows the port

Since the CQRS split, `TransactionalUseCaseFactory` no longer takes a set of read-only method *names*. A query port is
read-only in its entirety and a command port is not, so the caller passes a constant. The hand-maintained list that
could silently drift out of step with a rename is gone — see [CQRS](./cqrs.md).

### Bean validation → the web adapter

Request DTOs live in `infrastructure/web/dto` with their `jakarta.validation` annotations, because those constraints are
enforced by Spring MVC and therefore describe an **HTTP**
contract. The use-case ports accept **commands** (`application/command`) built by the controller — already parsed and
typed, so a use case can never receive a half-validated request.

Validation is split, not duplicated:

| Kind                                               | Enforced by         |
|----------------------------------------------------|---------------------|
| Syntactic (required, length, e-mail)               | web adapter         |
| Invariants (name not blank, complete translations) | domain constructors |

### SLF4J → a port

`UseCaseLogger` in the application layer, `Slf4jUseCaseLogger` in infrastructure. Arguable — SLF4J is a facade, not a
framework — but it makes the dependency rule a single sentence with no exceptions, which a build check can enforce.

### Spring's `OptimisticLockingFailureException` → `VersionGuard`

The shared helper threw a Spring exception, which would have pulled the framework back in through the back door. The
check now lives in the domain as `VersionGuard.requireMatch`, and the caller supplies the exception to throw.

### Spring Data out of the ports

`UserUseCase.getAllUsers` took a `Pageable` and returned a `Page`. Both are now the domain's own `PageRequest` /
`PageResult`; the conversion happens in the controller. The same applied to `EmailUseCase.getEmailLogs`.

### Spring configuration out of the use cases

`EmailService` was injected with mail-service's whole `@ConfigurationProperties` object to read a single field. It now
takes a `SenderAddress` value object, and `MailProperties` moved to infrastructure — SMTP host, port and password are no
longer in reach of code that has no business knowing them.

## Status

| Service                | Framework-free application layer                                         |
|------------------------|--------------------------------------------------------------------------|
| `project-service`      | yes                                                                      |
| `iam-service`          | yes                                                                      |
| `mail-service`         | yes                                                                      |
| `template-service`     | yes — so every future service starts this way                            |
| `task-service`         | **no** — cloned before the refactor; still `@Service` / `@Transactional` |
| `notification-service` | **no** — same                                                            |

`task-service` and `notification-service` were generated from `template-service` *before* the framework-free refactor,
so they carry the old shape. Re-clone them from the current template rather than porting by hand — the template now
includes the error catalogue, the logging port, the transactional decorator, the bean configuration and the CQRS port
split.

The remaining external imports in every application layer are the three saga contract types plus `java.time` and
`java.util`.

## Error catalogues

Each context owns one, in `domain/error/`:

| Service            | Catalogue       |
|--------------------|-----------------|
| `project-service`  | `ProjectError`  |
| `iam-service`      | `IamError`      |
| `mail-service`     | `MailError`     |
| `template-service` | `TemplateError` |

One choice worth naming: IAM maps both "no such user" and "wrong password" to
`iam.auth.invalid_credentials`. Telling them apart would be a user-enumeration oracle.
