import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    group = "com.vertyll.projecta"
    version = "0.0.1-SNAPSHOT"
    description = "Microservices architecture based on Apache Kafka"
    
    extra["author"] = "Mikołaj Gawron"
    extra["email"] = "gawrmiko@gmail.com"
    
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
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(rootProject.libs.spring.boot.dependencies.get().toString())
            mavenBom(rootProject.libs.spring.cloud.dependencies.get().toString())
            mavenBom(rootProject.libs.testcontainers.bom.get().toString())
        }
    }

    // Common dependencies for all subprojects
    dependencies {
        add("implementation", rootProject.libs.bundles.spring.boot.common)
        add("testImplementation", rootProject.libs.bundles.test.common)
    }

    // Configure ktlint
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(false)
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(false)
        enableExperimentalRules.set(true)
        filter {
            exclude { element -> element.file.path.contains("generated/") }
            include("**/src/**/*.kt")
            include("**/src/**/*.kts")
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xjsr305=strict", 
                "-Xannotation-default-target=param-property"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
