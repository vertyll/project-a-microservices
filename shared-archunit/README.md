# shared-archunit

The architecture rules every service is checked against, as executable tests.

A service applies them with one class:

```kotlin
class ProjectArchitectureTest : VedsArchitectureTest("com.vertyll.veds.project")
```

See [Shared Modules](../docs/shared-modules.md) for how this module relates to the others, and
[Hexagonal Layering](../docs/hexagonal-layering.md) for the rule it exists to defend.

## What it checks

| Rule                                         | Guards                                                         |
|----------------------------------------------|----------------------------------------------------------------|
| The dependency rule points inwards           | `infrastructure` → `application` → `domain`, never the reverse |
| The domain layer is framework-free           | no Spring, JPA, Jackson, Kafka, Avro or SLF4J in `domain`      |
| The application layer is framework-free      | the same for `application`                                     |
| JPA entities live in the persistence adapter | `@Entity` only under `infrastructure.persistence.entity`       |
| Controllers live in the web adapter          | `@RestController` only under `infrastructure.web`              |
| Only infrastructure talks to Kafka           | the inside states intent through a port; the outbox carries it |
| Application ports are interfaces             | a port is a contract, not a class the inside instantiates      |
| Repository ports are interfaces              | the domain says what it needs, the adapter how                 |
| Adapters live in infrastructure              | an adapter is by definition the outside edge                   |

Plus ArchUnit's own `GeneralCodingRules`: no standard-stream writes, no generic exceptions, no
`java.util.logging`, no field injection.

## Why it exists next to `checkHexagonalDependencies`

The Gradle task reads the application layer's resolved **classpath**, so it catches a framework
arriving through a dependency — including transitively. It cannot see anything finer. These rules
work at class level, so they catch what a classpath cannot express: a JPA annotation on a domain
model, a controller outside the web adapter, a port that is a class.

The two overlap on purpose. The classpath check fails the build earlier and with a clearer
message; ArchUnit covers the cases it structurally cannot reach.

## Two deliberate exemptions

- **Kotlin compiler artifacts.** `DefaultImpls` and `WhenMappings` are synthesized, not written.
  `DefaultImpls` is a class, so an "interfaces only" rule would fail on every interface that has a
  default method.
- **The Spring Boot application class.** It is the composition root and carries `@EnableKafka`,
  which is wiring rather than a dependency the inside took on.
