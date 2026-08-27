# shared-messaging-kafka

The transactional outbox, idempotent consumption and Avro serialization over Kafka.

This is a Spring module. Nothing in a service's application layer may depend on it — that is
what the framework-free libraries exist to prevent, and what `checkHexagonalDependencies`
enforces.

See [Shared Modules](../docs/shared-modules.md) for how this module relates to the others.

## What lives here, and why it is shared

| Area                                                  | Why it is not per service                                                                                 |
|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Transactional outbox, dispatcher, `BaseOutbox`        | The atomicity guarantee is the same everywhere; a second implementation is a second place to get it wrong |
| Consumer deduplication, `BaseProcessedEvent`          | Exactly-once handling is a property of the transport, not of any one domain                               |
| Avro serializer, deserializer, Schema Registry wiring | Wire format, not domain                                                                                   |

Anything that encodes a *decision about a domain* does not belong here. The temptation is to
put a "shared" role name or status enum in, and that is the shared-kernel antipattern: two
bounded contexts then cannot evolve their vocabulary independently.

## Who takes it

Every service that publishes or consumes an integration event. `api-gateway` does not, and
neither does `translation-service` — it publishes nothing, so it carries no outbox tables
either.

## API documentation

Published with Dokka:

```bash
./gradlew docs
```

Output lands in `docs/dokka/`. Its KDoc is the reference — the code here is read by every
service, so its contracts matter more than its implementation.
