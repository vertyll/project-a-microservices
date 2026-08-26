plugins {
    base
}

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

fun aggregator(name: String, taskGroup: String, desc: String, dependsOnTask: String = name) {
    tasks.register(name) {
        group = taskGroup
        description = desc
        gradle.includedBuilds
            .filterNot { it.name.endsWith("-contracts") }
            .filterNot { it.name.startsWith("shared-") }
            .filterNot { it.name.contains("api-gateway") }
            .forEach { dependsOn(it.task(":$dependsOnTask")) }
    }
}

aggregator("ktlintCheck",  "verification", "Runs ktlintCheck on all included builds")
aggregator("ktlintFormat", "formatting",   "Runs ktlintFormat on all included builds")
aggregator("detekt",       "verification", "Runs detekt on all included builds")
aggregator("test",         "verification", "Runs all tests across all included builds")
aggregator("checkHexagonalDependencies", "verification", "Fails if a framework reaches any application layer")

tasks.named("build") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":build")) }
}

tasks.named("clean") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":clean")) }
}

tasks.named("check") {
    dependsOn("ktlintCheck", "detekt", "test")
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
