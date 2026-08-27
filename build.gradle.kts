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

tasks.register("docs") {
    group = "documentation"
    description = "Generates Dokka HTML docs for the shared libraries (output: docs/dokka/index.html)"
    dependsOn(gradle.includedBuild("shared-infrastructure").task(":dokkaGenerate"))
    dependsOn(gradle.includedBuild("shared-translation").task(":dokkaGenerate"))
    doLast {
        listOf(
            "docs/dokka/index.html",
            "docs/dokka/shared-translation/index.html",
        ).map(rootDir::resolve)
            .filter { it.exists() }
            .forEach { logger.lifecycle("Dokka HTML docs: ${it.toURI()}") }
    }
}
