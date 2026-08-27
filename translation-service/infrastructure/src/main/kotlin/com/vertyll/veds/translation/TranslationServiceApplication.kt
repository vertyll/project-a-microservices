package com.vertyll.veds.translation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(
    "com.vertyll.veds.translation",
)
@EnableJpaRepositories(
    "com.vertyll.veds.translation.infrastructure.persistence.repository",
)
@EntityScan(
    "com.vertyll.veds.translation.infrastructure.persistence.entity",
)
class TranslationServiceApplication

fun main(args: Array<String>) {
    runApplication<TranslationServiceApplication>(*args)
}
