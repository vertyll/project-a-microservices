plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

// This creates a JAR without a main class (library)
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    // Web dependencies
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")

    // Event bus
    api("org.springframework.kafka:spring-kafka")

    // Security
    api("org.springframework.boot:spring-boot-starter-security")
    api("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Database
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Utilities
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("io.github.microutils:kotlin-logging:3.0.5")
    api("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI documentation
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}