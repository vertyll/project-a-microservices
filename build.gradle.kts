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
 * Every shared library that publishes KDoc, and the blurb shown for it on the landing page.
 * A module joins the published documentation by being added here.
 */
val documentedLibraries =
    listOf(
        "shared-saga-api" to "Framework-free saga vocabulary spoken by the application layers",
        "shared-translation" to "Translation key DSL and the ICU message renderer",
        "shared-web" to "Keycloak authentication, HTTP concurrency helpers and shared configuration",
        "shared-messaging-kafka" to "Transactional outbox, idempotent consumption and Avro over Kafka",
        "shared-saga-engine" to "Saga orchestration, compensation and the JPA flavour of its ports",
        "shared-translation-client" to "Start-up registration of a service's translation keys",
    )

tasks.register("docs") {
    group = "documentation"
    description = "Generates Dokka HTML docs for the shared libraries (output: docs/dokka/index.html)"

    documentedLibraries.forEach { (name, _) ->
        dependsOn(gradle.includedBuild(name).task(":dokkaGenerate"))
    }

    val landingPage = rootDir.resolve("docs/dokka/index.html")
    val libraries = documentedLibraries

    doLast {
        val cards =
            libraries.joinToString("\n") { (name, blurb) ->
                """      <li><a href="$name/index.html"><code>$name</code></a> — $blurb</li>"""
            }
        landingPage.parentFile.mkdirs()
        landingPage.writeText(
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>veds — shared library API documentation</title>
                <style>
                  :root { color-scheme: light dark; }
                  body { font-family: system-ui, sans-serif; max-width: 42rem; margin: 4rem auto; padding: 0 1rem; line-height: 1.6; }
                  li { margin-bottom: .5rem; }
                </style>
              </head>
              <body>
                <h1>veds — shared library API documentation</h1>
                <p>Generated from KDoc with Dokka.</p>
                <ul>
            $cards
                </ul>
              </body>
            </html>
            """.trimIndent(),
        )
        logger.lifecycle("Dokka HTML docs: ${landingPage.toURI()}")
    }
}
