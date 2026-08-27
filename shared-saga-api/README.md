# shared-saga-api

The saga vocabulary: `Saga`, `SagaStep`, `SagaStatus`, `SagaStepStatus` and `SagaTypeValue`.

Depends on the Kotlin standard library and nothing else, deliberately. These types are named by
the **application layer** of every service that runs a saga, and that layer must stay free of
frameworks. The module boundary turns the hexagonal rule into a compile error rather than a
check that runs afterward.

The Spring-bound engine that drives these types lives in
[`shared-saga-engine`](../shared-saga-engine/README.md) and depends on this module. Never the
other way round.

See [Shared Modules](../docs/shared-modules.md) for the full picture.

## API documentation

```bash
./gradlew docs
```
