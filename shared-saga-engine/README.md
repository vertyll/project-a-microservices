# shared-saga-engine

Saga orchestration: the engine, compensation, the watchdog, and the JPA flavor of the saga
ports (`BaseSaga`, `BaseSagaStep`, `BaseSagaRepository`).

Depends on [`shared-saga-api`](../shared-saga-api/README.md) for its vocabulary and on
`shared-messaging-kafka` for the outbox — compensation is published transactionally rather than
straight to Kafka.

See [Shared Modules](../docs/shared-modules.md) for how this module relates to the others.

## Why the engine is shared

The saga protocol has to be identical across services, or a compensation written in one cannot
be reasoned about from another. The storage flavor is not: the engine talks to ports, so a
service could implement them against a different database without the engine changing.

## Who takes it

Only services that actually run a saga. `file-service` publishes events but orchestrates
nothing, so it takes the outbox without this module and carries no saga tables.

## API documentation

```bash
./gradlew docs
```
