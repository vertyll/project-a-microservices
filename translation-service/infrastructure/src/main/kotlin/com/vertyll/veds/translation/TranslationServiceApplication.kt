package com.vertyll.veds.translation

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
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
    "com.vertyll.veds.translation",
    "com.vertyll.veds.sharedinfrastructure",
)
@EnableJpaRepositories(
    "com.vertyll.veds.translation.infrastructure.persistence.repository",
)
@EntityScan(
    "com.vertyll.veds.translation.infrastructure.persistence.entity",
)
@EnableKafka
class TranslationServiceApplication

fun main(args: Array<String>) {
    runApplication<TranslationServiceApplication>(*args)
}
