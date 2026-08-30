# Translations

Three different things get called "translation" in this system, and they work differently.

| Kind                                       | Owner                      | Storage                                     | Missing value renders as          |
|--------------------------------------------|----------------------------|---------------------------------------------|-----------------------------------|
| UI copy (errors, labels, validation)       | code declares, admin edits | `translation-service`                       | **the key itself**                |
| Data labels (project categories, statuses) | the user who typed them    | the owning service                          | the language the author did write |
| Mail bodies                                | code declares, admin edits | `translation-service`, rendered server-side | the key itself                    |

## Why a missing key renders as the key

Not a fallback. A fallback substitutes another language and the reader assumes the text is finished. A bare
`project.member_removed` on screen is the opposite: visible, greppable, impossible to miss in review, and it names
exactly what has to be fixed.

Throwing instead would take a page down over one absent string — for a defect the CI key check should already have
caught before deployment.

Data labels are the one exception, and for a concrete reason: a category is identified by a UUID, so there is no key to
print. `a3f2e1b4-…` on screen names nothing. The read returns what the author did write and the response reports which
language that is, so the client can mark it.

## Ownership

**Code owns the keys. Administrators own the text.**

Each service declares its keys with the DSL in `shared-translation`:

```kotlin
val catalogue = translations("project-service") {
    key("project.member_count") {
        pl("{count, plural, one{# członek} few{# członków} many{# członków} other{# członka}}")
        en("{count, plural, one{# member} other{# members}}")
    }
}
```

At start-up the service registers this with `translation-service`. Registration is additive:
it inserts new keys and refreshes shipped defaults, and **never touches an override**.

That is why `default_value` and `override_value` are two columns. One column written by both sides would mean every
redeploy silently reverts somebody's correction — the single most likely way an editable-translations feature turns into
a liability.

There is no API to create or delete a key. Deleting one would leave the emitting code with nothing to render; creating
one would produce a key nothing ever asks for.

### Where `common.*` lives

Declared once, in `translation-service`, not in every service. Registration refuses a key another service already owns —
deliberately, since two contexts claiming the same message means whichever deploys last silently wins. Every service
declaring its own `common.version_mismatch`
would hit exactly that rule, so the platform catalogue owns the platform's keys.

### Registration failure does not stop a service

A catalogue that could not be published is a degraded state: clients use whatever the catalogue already holds, and a
genuinely missing key renders as the key. Refusing to start would mean a brief `translation-service` outage takes every
service down with it. Registration is retried on the next restart.

## Languages

Seeded from code, same as keys. `LanguageCode` used to be an enum in project-service and task-service, and every
translatable aggregate required a value for all of its constants. That was wrong in a specific way: the day a language
was added, every stored row became invalid and the next write to it threw.

Now the tag is a `LanguageTag` value class (BCP 47, normalized to lower case) and the catalogue lives in
`translation-service`. Normalisation is not cosmetic — without it `pl`, `PL` and
`pl-PL` become three languages nobody ever reconciles.

Completeness of a *submitted* label is still enforced, in the application layer against the seeded catalogue. The
guarantee is unchanged; the side effect is gone.

## ICU, and why not `java.text.MessageFormat`

`MessageFormat` has no CLDR plural rules, so Polish `one/few/many/other` — "1 zadanie",
"3 zadania", "5 zadań" — is not expressible. `shared-translation` uses **ICU4J**, and the front end uses
`ngx-translate-messageformat-compiler` for the same reason.

Patterns are validated twice: when a service declares them (start-up, in the owning service)
and when an administrator saves one (rejected with a reason). An unbalanced brace in
`{count, plural, one{…}` does not fail quietly — it throws at render time, in the middle of a page nobody was editing.

## The public endpoint

`GET /translations/{language}` is unauthenticated and cacheable. Unauthenticated because the login screen needs its
labels before anybody has a token; cacheable because it is hit by every application start, in every tab and every
service that renders an e-mail.

The ETag is `MAX(updated_at)` for the language — cheaper than hashing the set, and monotonic:
any write moves it, so a client holding the current marker is holding the current translations.

`GET /translations/languages` is what stops every other component compiling its own list.

## The language header

The client states its language in **`X-Lang`**, not `Accept-Language`.

`Accept-Language` is a forbidden header name in the Fetch standard, so a browser refuses to let JavaScript set it — an
application can only ever send the browser's locale that way, never the language the user actually picked in the
interface. A custom header is the only way to carry a deliberate choice.

The front end was already sending `X-Lang`; the back end was reading `Accept-Language`, so the header the user chose was
being discarded and the controllers were falling back on nothing.

### Key naming

Every key is prefixed with the context that owns it:

| Shape                    | Owner                          | Example                              |
|--------------------------|--------------------------------|--------------------------------------|
| `common.*`               | platform (translation-service) | `common.version_mismatch`            |
| `<context>.*`            | that bounded context           | `project.invitation.expired`         |
| `validation.<context>.*` | that bounded context           | `validation.iam.password_complexity` |

Validation keys were originally unprefixed and auto-generated from English sentences —
`validation.email_is_required` alongside `validation.email_required`, three naming styles at once. Worse, several
services declared the same unprefixed key, which registration refuses:
whichever deployed first would have claimed it and the rest would have failed to register. Prefixing removes the
conflict by making ownership explicit rather than accidental.

## The front end

`ApiTranslateLoader` replaces the bundled JSON loader: the catalogue is editable at runtime, so a build artifact would
freeze it and an administrator's correction would never reach anybody.

`MissingTranslationKeyHandler` renders a missing key as the key. `ngx-translate` does this by default; it is declared
explicitly so the behavior is a reviewed decision rather than a default nobody looked at.

`LanguageService` fetches `GET /translations/languages` during start-up. The list of languages is no longer a constant
in the front end — a language seeded in the back end appears without a front-end release. The category and status forms
iterate over exactly this list when asking for a label in every language.

`ngx-translate-messageformat-compiler` renders the ICU patterns; without it Polish `few`/`many`
would not work on the client either.

## Administration

`/admin/translations/**`, behind the **global** `ADMIN` realm role — not the project-scoped policy used elsewhere.
Editing translations is not something one can be permitted inside one project and not another, so the per-project model
would be the wrong shape.

- keys are listed with both columns and the languages they still lack
- an override can be dropped, reverting to what the owning service ships, without the editor needing to know the
  original text
- `updated_by` is recorded on every override

### Spreadsheet import and export

`GET /admin/translations/export` produces an `.xlsx` with a fixed, positional layout: key, source service, description,
then one column per language.

**Read by position, not by header text.** The headers are themselves translated, so a file exported in Polish and
re-imported would not match headers looked up in English. Positional reading also means the file survives a translator
renaming a heading.

Headers are resolved **on the server**, in the language from `X-Lang`, because the file is produced there — sending raw
keys would put `translation.export.column.key` in a column heading. The language columns are headed by each language's
own display name, which needs no translation and cannot go missing.

A blank cell is skipped, not imported as an empty string: leaving a cell alone means
"unchanged", not "clear this translation".

`SXSSFWorkbook` rather than `XSSFWorkbook`, since a few thousand keys times a handful of languages is a large sheet and
the streaming writer keeps only a window of rows in memory.

Import reports rather than fails: a spreadsheet of a few thousand rows with three bad ones applies the rest and names
what was skipped. Rejecting the file wholesale would mean a translator fixing one cell and re-uploading everything. Rows
for unknown keys and unknown languages are reported, never created — both sets are owned by code.
