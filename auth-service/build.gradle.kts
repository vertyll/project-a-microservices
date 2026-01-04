val springdocOpenApiVersion = "2.3.0"
val jjwtVersion = "0.13.0"

dependencies {
    apply(plugin = "kotlin-jpa")

    // Project dependencies
    implementation(project(":shared-infrastructure"))

    // Spring Boot - implementation
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Cloud - implementation
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // JWT - implementation
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Kafka - implementation
    implementation("org.springframework.kafka:spring-kafka")

    // OpenAPI documentation - implementation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenApiVersion")

    // Database - runtimeOnly
    runtimeOnly("org.postgresql:postgresql")

    // Dev tools - developmentOnly
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing - testImplementation
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
