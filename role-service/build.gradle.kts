dependencies {
    implementation(project(":shared-infrastructure"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // OpenAPI documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Actuator for health checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Dev
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
