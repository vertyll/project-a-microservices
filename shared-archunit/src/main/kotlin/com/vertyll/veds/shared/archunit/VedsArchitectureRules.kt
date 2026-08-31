package com.vertyll.veds.shared.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture

/**
 * The architecture rules every service is held to, expressed as executable checks.
 *
 * They restate what `docs/hexagonal-layering.md`, `docs/eventual-consistency.md` and
 * `docs/shared-modules.md` describe in prose. `checkHexagonalDependencies` already stops a
 * framework from reaching the application layer's *classpath*; these rules work at class level,
 * so they also catch what a shared classpath cannot express — a JPA annotation on a domain
 * model, a controller outside the web adapter, a saga step publishing straight to Kafka.
 *
 * A service applies them by extending `VedsArchitectureTest`.
 */
object VedsArchitectureRules {
    /** Packages a domain or application class may never touch. */
    private val FRAMEWORK_PACKAGES =
        arrayOf(
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.validation..",
            "org.hibernate..",
            "com.fasterxml.jackson..",
            "tools.jackson..",
            "org.apache.kafka..",
            "org.apache.avro..",
            "io.confluent..",
            "org.slf4j..",
        )

    fun layering(base: String): ArchRule =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer(DOMAIN)
            .definedBy("$base.domain..")
            .layer(APPLICATION)
            .definedBy("$base.application..")
            .layer(INFRASTRUCTURE)
            .definedBy("$base.infrastructure..")
            .whereLayer(INFRASTRUCTURE)
            .mayNotBeAccessedByAnyLayer()
            .whereLayer(APPLICATION)
            .mayOnlyBeAccessedByLayers(INFRASTRUCTURE)
            .whereLayer(DOMAIN)
            .mayOnlyBeAccessedByLayers(APPLICATION, INFRASTRUCTURE)
            .because("the dependency rule points inwards: infrastructure knows the inside, the inside knows nothing of it")

    fun domainIsFrameworkFree(base: String): ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$base.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*FRAMEWORK_PACKAGES)
            .because("the domain is the one layer that must outlive any framework choice")

    fun applicationIsFrameworkFree(base: String): ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$base.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*FRAMEWORK_PACKAGES)
            .because("use cases are constructible without a container, which is what makes them unit-testable")

    fun jpaEntitiesLiveInPersistence(base: String): ArchRule =
        classes()
            .that()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideInAPackage("$base.infrastructure.persistence.entity..")
            .because("persistence is an adapter detail, not a place the domain may leak into")

    fun controllersLiveInWebAdapter(base: String): ArchRule =
        classes()
            .that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should()
            .resideInAPackage("$base.infrastructure.web..")
            .because("HTTP is one delivery mechanism among several, so it belongs in its own adapter")

    fun kafkaOnlyInInfrastructure(base: String): ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("$base.infrastructure..")
            .and()
            .areNotAnnotatedWith("org.springframework.boot.autoconfigure.SpringBootApplication")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.apache.kafka..", "org.springframework.kafka..")
            .because("the broker is a delivery detail; the inside states intent through a port and the outbox carries it")

    fun portsAreInterfaces(base: String): ArchRule =
        classes()
            .that()
            .resideInAPackage("$base.application.port..")
            .should()
            .beInterfaces()
            .because("a port is a contract the outside implements, never a class the inside instantiates")

    fun repositoryPortsAreInterfaces(base: String): ArchRule =
        classes()
            .that()
            .resideInAPackage("$base.domain.repository..")
            .should()
            .beInterfaces()
            .because("the domain states what it needs; the adapter decides how")

    fun adaptersLiveInInfrastructure(base: String): ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Adapter")
            .should()
            .resideInAPackage("$base.infrastructure..")
            .because("an adapter is by definition the outside edge")

    fun all(base: String): List<ArchRule> =
        listOf(
            layering(base),
            domainIsFrameworkFree(base),
            applicationIsFrameworkFree(base),
            jpaEntitiesLiveInPersistence(base),
            controllersLiveInWebAdapter(base),
            kafkaOnlyInInfrastructure(base),
            portsAreInterfaces(base),
            repositoryPortsAreInterfaces(base),
            adaptersLiveInInfrastructure(base),
        )

    fun check(
        classes: JavaClasses,
        base: String,
    ) = all(base).forEach { it.check(classes) }

    private const val DOMAIN = "Domain"
    private const val APPLICATION = "Application"
    private const val INFRASTRUCTURE = "Infrastructure"
}
