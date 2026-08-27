# shared-infrastructure

The Spring-side machinery every service needs: the saga engine, the transactional outbox, Avro
serialization, the Keycloak JWT converter and ETag helpers.

Unlike `shared-contracts` and `shared-translation`, this module **is** a Spring module. Nothing
in a service's application layer may depend on it — that is what the two smaller libraries exist
to prevent, and what `checkHexagonalDependencies` enforces.

## What lives here, and why it is shared

| Area                                                  | Why it is not per service                                                                                        |
|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Saga engine, step recording, watchdog                 | The protocol has to be identical, or a compensation written in one service cannot be reasoned about from another |
| Transactional outbox and dispatcher                   | The atomicity guarantee is the same everywhere; a second implementation is a second place to get it wrong        |
| Avro serializer, deserializer, Schema Registry wiring | Wire format, not domain                                                                                          |
| Keycloak JWT authentication converter                 | One mapping from realm roles to authorities                                                                      |
| ETag utilities                                        | The `If-Match` contract must match across every endpoint                                                         |

Anything that encodes a *decision about a domain* does not belong here. The temptation is to
put a "shared" role name or status enum in, and that is the shared-kernel antipattern: two
bounded contexts then cannot evolve their vocabulary independently.

## API documentation

This module is published with Dokka:

```bash
./gradlew docs
```

Output lands in `docs/dokka/`. Its KDoc is the reference — the code here is read by every
service, so its contracts matter more than its implementation.
