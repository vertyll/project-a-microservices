plugins {
    alias(libs.plugins.kotlin.jvm)
    // Required by the root aggregate: it asks every included build that is not a
    // `-contracts` module for ktlintCheck, detekt and test, and a missing task
    // fails the whole run rather than skipping this module.
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Translation key declaration DSL and ICU message rendering, shared across services"

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
    // ICU4J, and nothing from Spring.
    //
    // `java.text.MessageFormat` cannot do this job: it has no CLDR plural rules,
    // so Polish `one/few/many/other` — "1 zadanie", "3 zadania", "5 zadań" —
    // is simply not expressible. ICU4J carries the CLDR data.
    //
    // This module is referenced by application layers, which must stay
    // framework-free, so it deliberately depends on nothing else.
    implementation(libs.kotlin.stdlib.jdk8)
    api(libs.icu4j)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dokka {
    moduleName.set("shared-translation")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka/shared-translation"))
    }
    dokkaSourceSets.named("main") {
        jdkVersion.set(25)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
    }
}
