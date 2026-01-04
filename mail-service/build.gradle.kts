val springdocOpenApiVersion = "2.3.0"

dependencies {
    apply(plugin = "kotlin-jpa")

    // Project dependencies
    implementation(project(":shared-infrastructure"))

    // Spring Boot - implementation
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Thymeleaf - implementation
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

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
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
