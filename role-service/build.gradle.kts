plugins {
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    // Project dependencies
    implementation(project(":shared-infrastructure"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.webmvc)

    // JWT
    implementation(libs.bundles.jjwt)

    // OpenAPI documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Database
    runtimeOnly(libs.postgresql)

    // Dev tools
    developmentOnly(libs.spring.boot.devtools)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.springframework.kafka.test)
}
