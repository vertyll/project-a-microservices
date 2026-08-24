package com.vertyll.veds.apigateway

import com.vertyll.veds.apigateway.session.GatewaySessionProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
    ],
)
@EnableConfigurationProperties(GatewaySessionProperties::class)
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
