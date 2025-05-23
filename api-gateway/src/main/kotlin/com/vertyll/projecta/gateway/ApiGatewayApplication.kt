package com.vertyll.projecta.gateway

import com.vertyll.projecta.common.config.SharedConfigProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.HttpStatus

@SpringBootApplication
@ComponentScan(
    basePackages = [
        "com.vertyll.projecta.common"
    ]
)
class ApiGatewayApplication(
    private val sharedConfig: SharedConfigProperties
) {
    @Value("\${server.port:8080}")
    private lateinit var serverPort: String

    companion object {
        // Route IDs
        private const val ROOT_REDIRECT_ROUTE = "root-redirect"
        private const val AUTH_SERVICE_ROUTE = "auth-service"
        private const val USER_SERVICE_ROUTE = "user-service"
        private const val ROLE_SERVICE_ROUTE = "role-service"
        private const val MAIL_SERVICE_ROUTE = "mail-service"

        // API Path Prefixes
        private const val AUTH_API_PATH = "/api/v1/auth/**"
        private const val USER_API_PATH = "/api/v1/users/**"
        private const val ROLE_API_PATH = "/api/v1/roles/**"
        private const val MAIL_API_PATH = "/api/v1/mail/**"

        // Rewrite path patterns
        private const val AUTH_REWRITE_PATTERN = "/api/v1/auth/(?<segment>.*)"
        private const val USER_REWRITE_PATTERN = "/api/v1/users/(?<segment>.*)"
        private const val ROLE_REWRITE_PATTERN = "/api/v1/roles/(?<segment>.*)"
        private const val MAIL_REWRITE_PATTERN = "/api/v1/mail/(?<segment>.*)"

        // Rewrite replacement patterns
        private const val AUTH_REPLACEMENT = "/api/auth/\${segment}"
        private const val USER_REPLACEMENT = "/api/users/\${segment}"
        private const val ROLE_REPLACEMENT = "/api/roles/\${segment}"
        private const val MAIL_REPLACEMENT = "/api/mail/\${segment}"
    }

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        val gatewayUrl = "http://localhost:$serverPort"

        val authServiceUrl = sharedConfig.services.authService.url
        val userServiceUrl = sharedConfig.services.userService.url
        val roleServiceUrl = sharedConfig.services.roleService.url
        val mailServiceUrl = sharedConfig.services.mailService.url

        return builder.routes()
            .route(ROOT_REDIRECT_ROUTE) { r ->
                r.path("/").filters { f -> f.redirect(HttpStatus.TEMPORARY_REDIRECT.value(), "/actuator/health") }
                    .uri(gatewayUrl)
            }
            .route(AUTH_SERVICE_ROUTE) { r ->
                r.path(AUTH_API_PATH)
                    .filters { f -> f.rewritePath(AUTH_REWRITE_PATTERN, AUTH_REPLACEMENT) }
                    .uri(authServiceUrl)
            }
            .route(USER_SERVICE_ROUTE) { r ->
                r.path(USER_API_PATH)
                    .filters { f -> f.rewritePath(USER_REWRITE_PATTERN, USER_REPLACEMENT) }
                    .uri(userServiceUrl)
            }
            .route(ROLE_SERVICE_ROUTE) { r ->
                r.path(ROLE_API_PATH)
                    .filters { f -> f.rewritePath(ROLE_REWRITE_PATTERN, ROLE_REPLACEMENT) }
                    .uri(roleServiceUrl)
            }
            .route(MAIL_SERVICE_ROUTE) { r ->
                r.path(MAIL_API_PATH)
                    .filters { f -> f.rewritePath(MAIL_REWRITE_PATTERN, MAIL_REPLACEMENT) }
                    .uri(mailServiceUrl)
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
