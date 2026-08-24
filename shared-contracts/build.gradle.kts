plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Framework-free contract types shared across services"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Nothing but the Kotlin standard library, deliberately.
    //
    // These types are referenced by the application layer of every service, and
    // that layer must stay free of frameworks. They used to live in
    // `shared-infrastructure`, which is a Spring module — importing a saga enum
    // pulled the whole framework onto the application classpath.
    //
    // The package names are unchanged (`com.vertyll.veds.sharedinfrastructure.saga.*`)
    // so that no existing import had to move; only the module boundary shifted.
    implementation(libs.kotlin.stdlib.jdk8)
}
