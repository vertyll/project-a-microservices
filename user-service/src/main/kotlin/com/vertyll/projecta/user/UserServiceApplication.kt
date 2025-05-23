package com.vertyll.projecta.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.user.domain.repository",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.user.domain.model",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EnableKafka
@ComponentScan(basePackages = ["com.vertyll.projecta.user", "com.vertyll.projecta.common"])
class UserServiceApplication

fun main(args: Array<String>) {
    runApplication<UserServiceApplication>(*args)
}
