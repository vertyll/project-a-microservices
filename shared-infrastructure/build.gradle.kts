plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
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

val jjwtVersion = "0.13.0"
val kotlinLoggingVersion = "3.0.5"

dependencies {
    // Web dependencies - api
    api("org.springframework.boot:spring-boot-starter-validation")

    // Configuration processor - kapt
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // Spring Boot - api
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-jackson")
    api("org.springframework.boot:spring-boot-starter-actuator")

    // Event bus - api
    api("org.springframework.kafka:spring-kafka")

    // Security - api & implementation
    api("org.springframework.boot:spring-boot-starter-security")
    api("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Database - api (needed for KafkaOutbox entity)
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.postgresql:postgresql")

    // Utilities - api
    api("tools.jackson.module:jackson-module-kotlin")
    api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
}
