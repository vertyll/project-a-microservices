package com.vertyll.projecta.role

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableJpaRepositories
@EnableKafka
class RoleServiceApplication

fun main(args: Array<String>) {
    runApplication<RoleServiceApplication>(*args)
}
