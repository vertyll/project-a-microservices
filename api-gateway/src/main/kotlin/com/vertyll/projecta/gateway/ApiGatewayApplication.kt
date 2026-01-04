package com.vertyll.projecta.gateway

import com.vertyll.projecta.sharedinfrastructure.config.SharedConfigAutoConfiguration
import com.vertyll.projecta.sharedinfrastructure.config.SharedConfigProperties
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaConfigAutoConfiguration
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.stereotype.Component

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
    ],
)
@Import(
    SharedConfigAutoConfiguration::class,
    KafkaConfigAutoConfiguration::class,
)
@ComponentScan(
    basePackages = [
        "com.vertyll.projecta.sharedinfrastructure",
        "com.vertyll.projecta.gateway",
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
            classes = [KafkaOutboxProcessor::class],
        ),
    ],
)
@EnableKafka
class ApiGatewayApplication(
    private val sharedConfig: SharedConfigProperties,
    private val authHeaderFilterFactory: AuthHeaderGatewayFilterFactory,
) {
    @Value($$"${server.port:8080}")
    private lateinit var serverPort: String

    companion object {
        // Service configuration
        private val SERVICE_CONFIGS =
            mapOf(
                "auth" to ServiceConfig("auth-service", "/api/v1/auth", "/auth", false),
                "user" to ServiceConfig("user-service", "/api/v1/users", "/users", true),
                "role" to ServiceConfig("role-service", "/api/v1/roles", "/roles", true),
                "mail" to ServiceConfig("mail-service", "/api/v1/mail", "/mail", true),
            )

        private const val ROOT_REDIRECT_ROUTE = "root-redirect"
        private const val CORS_HEADER = "Access-Control-Allow-Origin"
        private const val CORS_VALUE = "*"
    }

    data class ServiceConfig(
        val routeId: String,
        val apiPath: String,
        val targetPath: String,
        val requiresAuth: Boolean,
    )

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        val gatewayUrl = "http://localhost:$serverPort"
        val authFilter = authHeaderFilterFactory.apply(AuthHeaderGatewayFilterFactory.Config())

        var routes =
            builder
                .routes()
                // Root redirect route
                .route(ROOT_REDIRECT_ROUTE) { r ->
                    r
                        .path("/")
                        .filters { f -> f.redirect(HttpStatus.TEMPORARY_REDIRECT.value(), "/swagger-ui.html") }
                        .uri(gatewayUrl)
                }
                // API Gateway OpenAPI documentation routes
                .route("api-docs") { r ->
                    r
                        .path("/api-docs/**")
                        .filters { f ->
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            f
                        }.uri(gatewayUrl)
                }.route("swagger-ui") { r ->
                    r
                        .path("/swagger-ui/**")
                        .filters { f ->
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            f
                        }.uri(gatewayUrl)
                }.route("webjars") { r ->
                    r
                        .path("/webjars/**")
                        .filters { f ->
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            f
                        }.uri(gatewayUrl)
                }

        // Dynamic service routes
        SERVICE_CONFIGS.forEach { (serviceName, config) ->
            val serviceUrl = getServiceUrl(serviceName)

            // Root path route (e.g., /api/v1/users -> /users)
            routes =
                routes.route("${config.routeId}-root") { r ->
                    r
                        .path(config.apiPath)
                        .filters { f ->
                            f.rewritePath(config.apiPath, config.targetPath)
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            if (config.requiresAuth) {
                                f.filter(authFilter)
                            }
                            f
                        }.uri(serviceUrl)
                }

            // Wildcard path route (e.g., /api/v1/users/** -> /users/**)
            routes =
                routes.route(config.routeId) { r ->
                    r
                        .path("${config.apiPath}/**")
                        .filters { f ->
                            f.rewritePath("${config.apiPath}/(?<segment>.*)", $$"$${config.targetPath}/${segment}")
                            if (config.requiresAuth) {
                                f.preserveHostHeader()
                            }
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            if (config.requiresAuth) {
                                f.filter(authFilter)
                            }
                            f
                        }.uri(serviceUrl)
                }

            // OpenAPI documentation routes for each service
            routes =
                routes.route("${config.routeId}-api-docs") { r ->
                    r
                        .path("${config.apiPath}/api-docs/**")
                        .filters { f ->
                            f.rewritePath("${config.apiPath}/api-docs/(?<segment>.*)", $$"/api-docs/${segment}")
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            f
                        }.uri(serviceUrl)
                }

            routes =
                routes.route("${config.routeId}-swagger-ui") { r ->
                    r
                        .path("${config.apiPath}/swagger-ui/**")
                        .filters { f ->
                            f.rewritePath("${config.apiPath}/swagger-ui/(?<segment>.*)", $$"/swagger-ui/${segment}")
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            f
                        }.uri(serviceUrl)
                }

            // Add route for webjars resources
            routes =
                routes.route("${config.routeId}-webjars") { r ->
                    r
                        .path("${config.apiPath}/webjars/**")
                        .filters { f ->
                            f.rewritePath("${config.apiPath}/webjars/(?<segment>.*)", $$"/webjars/${segment}")
                            f.preserveHostHeader()
                            f.addResponseHeader(CORS_HEADER, CORS_VALUE)
                            f
                        }.uri(serviceUrl)
                }
        }

        return routes.build()
    }

    private fun getServiceUrl(serviceName: String): String =
        when (serviceName) {
            "auth" -> sharedConfig.services.authService.url
            "user" -> sharedConfig.services.userService.url
            "role" -> sharedConfig.services.roleService.url
            "mail" -> sharedConfig.services.mailService.url
            else -> throw IllegalArgumentException("Unknown service: $serviceName")
        }
}

@Component
class AuthHeaderGatewayFilterFactory : AbstractGatewayFilterFactory<AuthHeaderGatewayFilterFactory.Config>(Config::class.java) {
    data class Config(
        val enabled: Boolean = true,
        val headerName: String = HttpHeaders.AUTHORIZATION,
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            if (!config.enabled) {
                return@GatewayFilter chain.filter(exchange)
            }

            val authHeader = exchange.request.headers.getFirst(config.headerName)

            if (authHeader.isNullOrBlank()) { // NOSONAR
                // Optionally handle missing auth header
                chain.filter(exchange)
            } else {
                // Forward the auth header - it's already present in the request
                chain.filter(exchange)
            }
        }
    }

    override fun name(): String = "AuthHeader"

    override fun shortcutFieldOrder(): List<String> = listOf("enabled", "headerName")
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
