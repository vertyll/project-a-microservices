plugins {
    base
}

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

/** Every included build that carries Kotlin — the `-contracts` builds only hold Avro schemas. */
val codeBuilds = gradle.includedBuilds.filterNot { it.name.endsWith("-contracts") }

/**
 * The hexagonal services.
 *
 * Only these register `checkHexagonalDependencies`, and only these tag their integration tests
 * out of the default `test` run, so `api-gateway` and the `shared-*` libraries are excluded from
 * both aggregations — asking them for a task they never registered would just fail the build.
 */
val serviceBuilds = codeBuilds.filter { it.name.endsWith("-service") }

/** Set by `./gradlew test -PintegrationTests` — see docs/testing.md. */
val integrationTests = hasProperty("integrationTests")

fun aggregator(
    name: String,
    taskGroup: String,
    desc: String,
    builds: List<IncludedBuild> = codeBuilds,
    dependsOnTask: String = name,
) {
    tasks.register(name) {
        group = taskGroup
        description = desc
        builds.forEach { dependsOn(it.task(":$dependsOnTask")) }
    }
}

aggregator("ktlintCheck", "verification", "Runs ktlintCheck on all included builds")
aggregator("ktlintFormat", "formatting", "Runs ktlintFormat on all included builds")
aggregator("detekt", "verification", "Runs detekt on all included builds")

aggregator(
    "checkHexagonalDependencies",
    "verification",
    "Fails if a framework reaches the application layer of any service",
    builds = serviceBuilds,
)

aggregator(
    "test",
    "verification",
    if (integrationTests) {
        "Runs unit and integration tests across the services that have them (-PintegrationTests)"
    } else {
        "Runs all tests across all included builds"
    },
    builds = if (integrationTests) serviceBuilds else codeBuilds,
)

tasks.named("build") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":build")) }
}

tasks.named("clean") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":clean")) }
}

tasks.named("check") {
    dependsOn("ktlintCheck", "detekt", "checkHexagonalDependencies", "test")
}

/**
 * Every shared library that publishes KDoc: its name, the blurb shown on the landing page, and
 * whether it is safe for an application layer to depend on. A module joins the published
 * documentation by being added here.
 */
data class DocumentedLibrary(
    val name: String,
    val blurb: String,
    val frameworkFree: Boolean,
)

val documentedLibraries =
    listOf(
        DocumentedLibrary("shared-saga-api", "The saga vocabulary spoken by the application layers", true),
        DocumentedLibrary("shared-translation", "Translation key DSL and the ICU message renderer", true),
        DocumentedLibrary("shared-web", "Keycloak authentication, HTTP concurrency helpers and shared configuration", false),
        DocumentedLibrary("shared-messaging-kafka", "Transactional outbox, idempotent consumption and Avro over Kafka", false),
        DocumentedLibrary("shared-saga-engine", "Saga orchestration, compensation and the JPA flavour of its ports", false),
        DocumentedLibrary("shared-translation-client", "Start-up registration of a service's translation keys", false),
    )

tasks.register("docs") {
    group = "documentation"
    description = "Generates Dokka HTML docs for the shared libraries (output: docs/dokka/index.html)"

    documentedLibraries.forEach { dependsOn(gradle.includedBuild(it.name).task(":dokkaGenerate")) }

    val landingPage = rootDir.resolve("docs/dokka/index.html")
    val libraries = documentedLibraries

    doLast {
        // Dokka writes one self-contained site per module. Without a landing page the published
        // root would be a bare directory listing.
        //
        // The template is trimmed *before* the generated sections are substituted in: an
        // interpolated block carries its own newlines, and `trimIndent` would then measure the
        // common indent as zero and strip nothing.
        fun cards(entries: List<DocumentedLibrary>) =
            entries.joinToString("\n") {
                """      <li><a href="${it.name}/index.html"><code>${it.name}</code></a><span>${it.blurb}</span></li>"""
            }

        val template =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>veds — shared library API documentation</title>
                <style>
                  :root {
                    color-scheme: light dark;
                    --fg: #1a1a1a; --muted: #5b5b5b; --line: #e2e2e2; --accent: #1f6feb;
                  }
                  @media (prefers-color-scheme: dark) {
                    :root { --fg: #e8e8e8; --muted: #a0a0a0; --line: #303030; --accent: #6aa9ff; }
                  }
                  body {
                    font-family: system-ui, -apple-system, sans-serif;
                    max-width: 46rem; margin: 0 auto; padding: 4rem 1.25rem;
                    line-height: 1.6; color: var(--fg);
                  }
                  h1 { font-size: 1.5rem; margin: 0 0 .25rem; }
                  h2 { font-size: .8rem; text-transform: uppercase; letter-spacing: .08em;
                       color: var(--muted); margin: 2.5rem 0 .75rem; font-weight: 600; }
                  p.lede { color: var(--muted); margin: 0 0 .5rem; }
                  ul { list-style: none; padding: 0; margin: 0; }
                  li { display: flex; flex-direction: column; gap: .15rem;
                       padding: .7rem 0; border-bottom: 1px solid var(--line); }
                  li span { color: var(--muted); font-size: .92rem; }
                  a { color: var(--accent); text-decoration: none; font-weight: 600; }
                  a:hover { text-decoration: underline; }
                  code { font-size: .95rem; }
                  footer { margin-top: 2.5rem; color: var(--muted); font-size: .88rem; }
                </style>
              </head>
              <body>
                <h1>veds — shared libraries</h1>
                <p class="lede">API documentation generated from KDoc with Dokka.</p>

                <h2>Framework-free — safe for an application layer</h2>
                <ul>
            @@PURE@@
                </ul>

                <h2>Spring — infrastructure layer only</h2>
                <ul>
            @@SPRING@@
                </ul>

                <footer>
                  What each module is responsible for, and why they are separate, is described in
                  <code>docs/shared-modules.md</code>.
                </footer>
              </body>
            </html>
            """.trimIndent()

        landingPage.parentFile.mkdirs()
        landingPage.writeText(
            template
                .replace("@@PURE@@", cards(libraries.filter { it.frameworkFree }))
                .replace("@@SPRING@@", cards(libraries.filterNot { it.frameworkFree })),
        )
        logger.lifecycle("Dokka HTML docs: ${landingPage.toURI()}")
    }
}
