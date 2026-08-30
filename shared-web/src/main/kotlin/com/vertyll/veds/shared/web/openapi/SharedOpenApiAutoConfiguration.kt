package com.vertyll.veds.shared.web.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Gives every service's generated OpenAPI document a name and a bearer-token scheme.
 *
 * Without this, springdoc emits its defaults — `OpenAPI definition` / `v0` — so eight services
 * publish eight documents that cannot be told apart, and Swagger UI has no Authorize button.
 * Since almost every endpoint requires a JWT, that made "Try it out" useless.
 *
 * Registered here rather than per service so the description stays identical everywhere; a
 * service that wants its own document simply declares its own [OpenAPI] bean.
 */
@Configuration
@ConditionalOnClass(OpenAPI::class)
internal class SharedOpenApiAutoConfiguration {
    private companion object {
        private const val BEARER_SCHEME = "bearer-jwt"
    }

    @Bean
    @ConditionalOnMissingBean
    fun sharedOpenApi(
        @Value("\${spring.application.name:veds-service}") applicationName: String,
        @Value("\${veds.shared.openapi.version:0.0.1-SNAPSHOT}") version: String,
    ): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title(applicationName)
                    .version(version)
                    .description("REST API of $applicationName. Endpoints require a Keycloak-issued bearer token unless stated otherwise."),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))
}
