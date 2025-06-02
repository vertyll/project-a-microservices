package com.vertyll.projecta.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.template.domain.repository",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.template.domain.model",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EnableKafka
@ComponentScan(
    basePackages = [
        "com.vertyll.projecta.template",
        "com.vertyll.projecta.common"
    ]
)
class TemplateServiceApplication

fun main(args: Array<String>) {
    runApplication<TemplateServiceApplication>(*args)
}
