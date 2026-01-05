dependencies {
    implementation(project(":shared-infrastructure"))

    // Spring Cloud Gateway & Microservices
    implementation(libs.bundles.spring.boot.gateway)

    // Kotlin extensions for reactive
    implementation(libs.bundles.gateway.kotlin)

    // Dev tools
    developmentOnly(libs.spring.boot.devtools)

    // JWT
    implementation(libs.bundles.jjwt)

    // OpenAPI documentation
    implementation(libs.springdoc.openapi.starter.webflux.ui)

    // Test dependencies
    testImplementation(libs.bundles.test.gateway)
}
