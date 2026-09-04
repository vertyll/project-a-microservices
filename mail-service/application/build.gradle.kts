plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":mail-domain"))
    implementation("com.vertyll.veds:shared-error")
    implementation("com.vertyll.veds:shared-saga-api")

    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/**
 * Fails the build if a framework reaches the application layer.
 *
 * The hexagonal dependency rule is only worth stating if something enforces it.
 * Without this check the layer stays clean exactly until the first person adds
 * `implementation(libs.bundles.spring.boot.common)` to fix a compile error, and
 * nothing objects.
 *
 * The check reads the resolved `compileClasspath`, not the declared
 * dependencies, so a framework arriving transitively is caught too. That path is
 * the easy one to miss: importing a single enum from a Spring-bound module puts
 * the whole framework on this layer's classpath.
 */
val forbiddenOnApplicationClasspath =
    listOf(
        "org.springframework",
        "jakarta.validation",
        "jakarta.persistence",
        "tools.jackson",
        "com.fasterxml.jackson",
        "org.slf4j",
        "org.apache.avro",
        "org.apache.kafka",
        "org.hibernate",
    )

val checkHexagonalDependencies =
    tasks.register("checkHexagonalDependencies") {
        group = "verification"
        description = "Asserts that no framework is on the application layer's compile classpath."

        val classpath = configurations.named("compileClasspath")

        doLast {
            val offenders =
                classpath
                    .get()
                    .resolvedConfiguration
                    .resolvedArtifacts
                    .asSequence()
                    .map { it.moduleVersion.id }
                    .filter { id -> forbiddenOnApplicationClasspath.any { id.group.startsWith(it) } }
                    .map { "${it.group}:${it.name}" }
                    .distinct()
                    .sorted()
                    .toList()

            if (offenders.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("The application layer must not depend on a framework.")
                        appendLine("Found on its compile classpath:")
                        offenders.forEach { appendLine("  - $it") }
                        appendLine()
                        appendLine("Move the framework concern into `infrastructure` and express it")
                        appendLine("as a port. See docs/hexagonal-layering.md.")
                    },
                )
            }
        }
    }

tasks.named("check") {
    dependsOn(checkHexagonalDependencies)
}
