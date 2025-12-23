package com.vertyll.projecta.mail

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.mail.domain.repository",
        "com.vertyll.projecta.sharedinfrastructure.kafka",
    ],
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.mail.domain.model",
        "com.vertyll.projecta.sharedinfrastructure.kafka",
    ],
)
@EnableKafka
@ComponentScan(
    basePackages = [
        "com.vertyll.projecta.mail",
        "com.vertyll.projecta.sharedinfrastructure",
    ],
)
class MailServiceApplication

fun main(args: Array<String>) {
    runApplication<MailServiceApplication>(*args)
}
