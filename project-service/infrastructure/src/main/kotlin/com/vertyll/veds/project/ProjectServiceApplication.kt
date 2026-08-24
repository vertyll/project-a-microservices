package com.vertyll.veds.project

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigAutoConfiguration
import com.vertyll.veds.sharedinfrastructure.translation.TranslationClientProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@Import(
    SharedConfigAutoConfiguration::class,
)
@ComponentScan(
    "com.vertyll.veds.project",
    "com.vertyll.veds.sharedinfrastructure",
)
@EnableJpaRepositories(
    "com.vertyll.veds.project.infrastructure.persistence.repository",
)
@EntityScan(
    "com.vertyll.veds.project.infrastructure.persistence.entity",
)
@EnableKafka
@EnableConfigurationProperties(TranslationClientProperties::class)
class ProjectServiceApplication

fun main(args: Array<String>) {
    runApplication<ProjectServiceApplication>(*args)
}
