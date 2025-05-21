package com.vertyll.projecta.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.vertyll.projecta.auth.domain.repository", "com.vertyll.projecta.common.saga", "com.vertyll.projecta.common.kafka"])
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.auth.domain.model",
        "com.vertyll.projecta.common.saga",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EnableKafka
@ComponentScan(basePackages = ["com.vertyll.projecta.auth", "com.vertyll.projecta.common"])
class AuthServiceApplication {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }
}

fun main(args: Array<String>) {
    runApplication<AuthServiceApplication>(*args)
}
