# translation-service

The translation catalogue: keys, languages and their text.

|           |         |
|-----------|---------|
| Port      | 8087    |
| Database  | 5438    |
| Publishes | nothing |
| Consumes  | nothing |

No outbox and no saga tables: it is a catalogue other services read and an administrator edits.

## Code owns the keys, administrators own the text

Each service declares its keys with the DSL in `shared-translation` and registers them at
start-up. Registration inserts what is new and refreshes shipped defaults; there is no API to
create or delete a key, because a key exists only because a branch throws it or a screen renders
it.

**`default_value` and `override_value` are separate columns.** One column written by both sides
would mean every redeploy silently reverts somebody's correction — the single most likely way an
editable-translations feature turns into a liability.

Registration refuses a key another service already owns. Two contexts claiming the same message
means whichever deploys last silently wins, so `common.*` is declared once, here.

## Rendering

ICU4J, not `java.text.MessageFormat`: the latter has no CLDR plural rules, so Polish
`one/few/many/other` is not expressible. Patterns are validated when a service declares them and
again when an administrator saves one — an unbalanced brace does not fail quietly, it throws at
render time in the middle of a page nobody was editing.

A key with no value renders as the key. Not a fallback: substituting another language looks like
a finished translation, whereas a bare key is greppable and names exactly what to fix.

## The public endpoint

`GET /translations/{language}` is unauthenticated — the login screen needs its labels before
anybody has a token — and cacheable. The ETag is `MAX(updated_at)` for the language: cheaper
than hashing the set, and monotonic, so a client holding the current marker holds the current
translations.

See [Translations](../docs/translations.md).
