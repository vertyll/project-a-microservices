plugins {
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    // Project dependencies
    implementation(project(":shared-infrastructure"))

    // Spring Boot with Security
    implementation(libs.bundles.spring.boot.webmvc.security)

    // Spring Cloud
    implementation(libs.spring.cloud.starter.openfeign)

    // JWT
    implementation(libs.bundles.jjwt)

    // OpenAPI documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Database
    runtimeOnly(libs.postgresql)

    // Dev tools
    developmentOnly(libs.spring.boot.devtools)

    // Testing
    testImplementation(libs.bundles.test.security)
}
