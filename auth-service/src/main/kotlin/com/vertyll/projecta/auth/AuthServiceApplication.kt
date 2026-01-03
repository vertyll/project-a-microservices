package com.vertyll.projecta.auth

import com.vertyll.projecta.sharedinfrastructure.config.SharedConfigAutoConfiguration
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootApplication
@Import(
    SharedConfigAutoConfiguration::class,
    KafkaConfigAutoConfiguration::class,
)
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.auth.domain.repository",
        "com.vertyll.projecta.sharedinfrastructure.kafka",
    ],
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.auth.domain.model",
        "com.vertyll.projecta.sharedinfrastructure.kafka",
    ],
)
@EnableKafka
@ComponentScan(
    basePackages = [
        "com.vertyll.projecta.auth",
        "com.vertyll.projecta.sharedinfrastructure",
    ],
)
class AuthServiceApplication {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager
}

fun main(args: Array<String>) {
    runApplication<AuthServiceApplication>(*args)
}
