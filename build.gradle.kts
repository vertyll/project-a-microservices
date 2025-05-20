import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.23" apply false
    kotlin("plugin.spring") version "1.9.23" apply false
    kotlin("plugin.jpa") version "1.9.23" apply false
}

allprojects {
    group = "com.vertyll.projecta"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
    }
    
    // Add Spring Cloud dependency management
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configurations {
        all {
            // Common dependencies for all subprojects
            dependencies {
                // Common dependencies for all subprojects
                add("implementation", "org.springframework.boot:spring-boot-starter")
                add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
                add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
                add("implementation", "org.springframework.kafka:spring-kafka")
                add("implementation", "io.github.microutils:kotlin-logging:3.0.5")
                
                // Test dependencies
                add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
                add("testImplementation", "org.springframework.kafka:spring-kafka-test")
                add("testImplementation", "org.testcontainers:junit-jupiter")
                add("testImplementation", "org.testcontainers:kafka")
                add("testImplementation", "org.testcontainers:postgresql")
            }
        }
    }
} 