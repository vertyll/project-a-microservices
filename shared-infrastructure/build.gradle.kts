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

dependencies {
    // Web dependencies
    api("org.springframework.boot:spring-boot-starter-validation")

    // Configuration processor
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Event bus
    api("org.springframework.kafka:spring-kafka")

    // Security
    api("org.springframework.boot:spring-boot-starter-security")
    api("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Database - needed for KafkaOutbox entity
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.postgresql:postgresql")

    // Utilities
    api("org.springframework.boot:spring-boot-starter-jackson")
    api("tools.jackson.module:jackson-module-kotlin")
    api("io.github.microutils:kotlin-logging:3.0.5")
    api("org.springframework.boot:spring-boot-starter-actuator")
}
