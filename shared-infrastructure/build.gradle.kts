plugins {
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.boot) apply false
}

// This creates a JAR without a main class (library)
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    // Shared infrastructure API dependencies
    api(libs.bundles.shared.infrastructure.api)

    // Configuration processor
    kapt(libs.spring.boot.configuration.processor)

    // JWT implementation (not API)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)

    // Database (needed for KafkaOutbox entity)
    api(libs.postgresql)
}
