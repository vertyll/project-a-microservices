package com.vertyll.projecta.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.template.domain.repository",
        "com.vertyll.projecta.sharedinfrastructure.kafka"
    ]
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.template.domain.model",
        "com.vertyll.projecta.sharedinfrastructure.kafka"
    ]
)
@EnableKafka
@ComponentScan(
    basePackages = [
        "com.vertyll.projecta.template",
        "com.vertyll.projecta.sharedinfrastructure"
    ]
)
class TemplateServiceApplication

fun main(args: Array<String>) {
    runApplication<TemplateServiceApplication>(*args)
}
