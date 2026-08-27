package com.vertyll.veds.apigateway.config

import com.vertyll.veds.apigateway.security.JsonAuthenticationEntryPoint
import com.vertyll.veds.shared.web.security.ReactiveKeycloakJwtAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
internal class SecurityConfig(
    private val jsonAuthenticationEntryPoint: JsonAuthenticationEntryPoint,
    private val reactiveKeycloakJwtConverter: ReactiveKeycloakJwtAuthenticationConverter,
) {
    companion object {
        private val PUBLIC_AUTH_ENDPOINTS =
            arrayOf(
                "/auth/register",
                "/auth/activate",
                "/auth/reset-password-request",
                "/auth/confirm-reset-password",
                "/auth/resend-activation",
                "/auth/authorize",
                "/auth/callback",
                "/auth/session",
                "/auth/logout",
            )

        private val SWAGGER_ENDPOINTS =
            arrayOf(
                "/swagger-ui.html",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
            )

        private const val ACTUATOR_ENDPOINTS = "/actuator/**"

        private val PROTECTED_AUTH_ENDPOINTS =
            arrayOf(
                "/auth/me",
                "/auth/me/permissions",
                "/auth/change-password-request",
                "/auth/change-email-request",
                "/auth/confirm-email-change",
                "/auth/confirm-password-change",
                "/auth/set-new-password",
            )

        private val ROLE_ADMIN_ENDPOINTS =
            arrayOf(
                "/roles/user/{userId}/role/{roleName}",
                "/roles/user/{userId}/role/{roleName}/",
            )
        private const val ROLE_USER_ENDPOINTS = "/roles/**"

        private const val PERMISSION_ENDPOINTS = "/permissions/**"

        private val USER_ADMIN_ENDPOINTS =
            arrayOf(
                "/users/admin/**",
            )
        private const val USER_PROFILE_ENDPOINT = "/users/me"
        private val USER_ID_ENDPOINT =
            arrayOf(
                "/users/{id}",
                "/users/{id}/",
                "/users/email/{email}",
                "/users/email/{email}/",
            )

        private const val MAIL_ENDPOINTS = "/mail/**"

        private val PROJECT_ENDPOINTS =
            arrayOf(
                "/projects/**",
                "/project-types/**",
                "/project-roles/**",
                "/project-user-roles/**",
            )

        private val TASK_ENDPOINTS = arrayOf("/tasks/**")

        private const val FILE_ENDPOINTS = "/files/**"

        private const val PUBLIC_TRANSLATION_ENDPOINTS = "/translations/**"

        private val NOTIFICATION_ENDPOINTS =
            arrayOf(
                "/notifications/**",
                "/ws/notifications/**",
            )
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(jsonAuthenticationEntryPoint)
            }.authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(*PUBLIC_AUTH_ENDPOINTS)
                    .permitAll()
                    .pathMatchers(*SWAGGER_ENDPOINTS)
                    .permitAll()
                    .pathMatchers(ACTUATOR_ENDPOINTS)
                    .permitAll()
                    .pathMatchers(*PROTECTED_AUTH_ENDPOINTS)
                    .authenticated()
                    .pathMatchers(*ROLE_ADMIN_ENDPOINTS)
                    .hasRole("ADMIN")
                    .pathMatchers(PERMISSION_ENDPOINTS)
                    .hasRole("ADMIN")
                    .pathMatchers(ROLE_USER_ENDPOINTS)
                    .authenticated()
                    .pathMatchers(*USER_ADMIN_ENDPOINTS)
                    .hasRole("ADMIN")
                    .pathMatchers(USER_PROFILE_ENDPOINT)
                    .authenticated()
                    .pathMatchers(*USER_ID_ENDPOINT)
                    .authenticated()
                    .pathMatchers(MAIL_ENDPOINTS)
                    .hasRole("ADMIN")
                    .pathMatchers(PUBLIC_TRANSLATION_ENDPOINTS)
                    .permitAll()
                    .pathMatchers(*PROJECT_ENDPOINTS)
                    .authenticated()
                    .pathMatchers(*TASK_ENDPOINTS)
                    .authenticated()
                    .pathMatchers(FILE_ENDPOINTS)
                    .authenticated()
                    .pathMatchers(*NOTIFICATION_ENDPOINTS)
                    .authenticated()
                    .anyExchange()
                    .authenticated()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(reactiveKeycloakJwtConverter)
                }
            }.build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
