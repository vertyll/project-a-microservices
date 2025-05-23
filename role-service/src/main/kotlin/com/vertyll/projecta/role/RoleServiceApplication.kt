package com.vertyll.projecta.role

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.role.domain.repository",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.role.domain.model",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EnableKafka
@ComponentScan(basePackages = ["com.vertyll.projecta.role", "com.vertyll.projecta.common"])
class RoleServiceApplication

fun main(args: Array<String>) {
    runApplication<RoleServiceApplication>(*args)
}
