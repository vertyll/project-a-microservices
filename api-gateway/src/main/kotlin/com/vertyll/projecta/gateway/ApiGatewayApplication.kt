package com.vertyll.projecta.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus

@SpringBootApplication
class ApiGatewayApplication {
    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("root-redirect") { r ->
                r.path("/")
                    .filters { f -> f.redirect(HttpStatus.TEMPORARY_REDIRECT.value(), "/actuator/health") }
                    .uri("http://localhost:8080")
            }
            .route("auth-service") { r ->
                r.path("/api/v1/auth/**")
                    .filters { f -> f.rewritePath("/api/v1/auth/(?<segment>.*)", "/api/auth/$\\{segment}") }
                    .uri("http://localhost:8085")
            }
            .route("user-service") { r ->
                r.path("/api/v1/users/**")
                    .filters { f -> f.rewritePath("/api/v1/users/(?<segment>.*)", "/api/users/$\\{segment}") }
                    .uri("http://localhost:8082")
            }
            .route("role-service") { r ->
                r.path("/api/v1/roles/**")
                    .filters { f -> f.rewritePath("/api/v1/roles/(?<segment>.*)", "/api/roles/$\\{segment}") }
                    .uri("http://localhost:8083")
            }
            .route("mail-service") { r ->
                r.path("/api/v1/mail/**")
                    .filters { f -> f.rewritePath("/api/v1/mail/(?<segment>.*)", "/api/mail/$\\{segment}") }
                    .uri("http://localhost:8084")
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
