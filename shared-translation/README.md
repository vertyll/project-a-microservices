# shared-translation

The key-declaration DSL and the ICU renderer, shared by every service.

Depends on ICU4J and the Kotlin standard library, and nothing else. It is referenced by
application layers, which must stay framework-free.

## Why ICU4J

`java.text.MessageFormat` has no CLDR plural rules, so Polish `one/few/many/other` — "1 zadanie",
"3 zadania", "5 zadań" — is simply not expressible. The front end uses
`ngx-translate-messageformat-compiler` for the same reason.

## What lives here

| Type                  | Role                                                                  |
|-----------------------|-----------------------------------------------------------------------|
| `translations { }`    | DSL a service uses to declare its keys and shipped defaults           |
| `IcuPatternValidator` | Refuses a pattern that will not compile, at declaration and at save   |
| `MessageResolver`     | Renders a key; a missing one renders as the key itself                |
| `TranslationSnapshot` | An immutable set for one language, with the version served as an ETag |

A duplicate key fails at start-up: two places believing they own the same message would make the
winner depend on declaration order.
