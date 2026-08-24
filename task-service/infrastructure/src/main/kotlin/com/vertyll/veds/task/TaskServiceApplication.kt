package com.vertyll.veds.task

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
    "com.vertyll.veds.task",
    "com.vertyll.veds.sharedinfrastructure",
)
@EnableJpaRepositories(
    "com.vertyll.veds.task.infrastructure.persistence.repository",
)
@EntityScan(
    "com.vertyll.veds.task.infrastructure.persistence.entity",
)
@EnableKafka
@EnableConfigurationProperties(TranslationClientProperties::class)
class TaskServiceApplication

fun main(args: Array<String>) {
    runApplication<TaskServiceApplication>(*args)
}
