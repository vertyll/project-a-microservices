package com.vertyll.projecta.gateway.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    companion object {
        // Public Auth endpoints
        private val PUBLIC_AUTH_ENDPOINTS = arrayOf(
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/activate",
            "/api/v1/auth/reset-password-request",
            "/api/v1/auth/confirm-reset-password",
            "/api/v1/auth/resend-activation"
        )
        
        // Swagger documentation endpoints
        private val SWAGGER_ENDPOINTS = arrayOf(
            "/swagger-ui.html", 
            "/api-docs/**", 
            "/v3/api-docs/**", 
            "/swagger-ui/**"
        )
        
        // Actuator endpoints
        private const val ACTUATOR_ENDPOINTS = "/actuator/**"
        
        // Protected Auth endpoints
        private val PROTECTED_AUTH_ENDPOINTS = arrayOf(
            "/api/v1/auth/me",
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password-request",
            "/api/v1/auth/change-email-request",
            "/api/v1/auth/confirm-email-change",
            "/api/v1/auth/confirm-password-change",
            "/api/v1/auth/set-new-password",
            "/api/v1/auth/sessions/**"
        )
        
        // Role endpoints
        private const val ROLE_ADMIN_ENDPOINTS = "/api/v1/roles/admin/**"
        private const val ROLE_USER_ENDPOINTS = "/api/v1/roles/**"
        
        // User endpoints
        private const val USER_ADMIN_ENDPOINTS = "/api/v1/users/admin/**"
        private const val USER_PROFILE_ENDPOINT = "/api/v1/users/me"
        private const val USER_ID_ENDPOINT = "/api/v1/users/{id}"
        
        // Mail endpoints
        private const val MAIL_ENDPOINTS = "/api/v1/mail/**"
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.csrf { it.disable() }  // Disable CSRF for API Gateway
            .formLogin { it.disable() }  // Disable default login form
            .httpBasic { it.disable() }  // Disable HTTP Basic Auth
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints that don't require authentication
                    .pathMatchers(*PUBLIC_AUTH_ENDPOINTS).permitAll()

                    // Swagger docs
                    .pathMatchers(*SWAGGER_ENDPOINTS).permitAll()

                    // Health and metrics endpoints
                    .pathMatchers(ACTUATOR_ENDPOINTS).permitAll()

                    // Auth service - some endpoints need authentication
                    .pathMatchers(*PROTECTED_AUTH_ENDPOINTS).authenticated()

                    // Role service admin endpoints
                    .pathMatchers(ROLE_ADMIN_ENDPOINTS).hasRole("ADMIN")

                    // Role service regular endpoints
                    .pathMatchers(ROLE_USER_ENDPOINTS).authenticated()

                    // User service admin endpoints
                    .pathMatchers(USER_ADMIN_ENDPOINTS).hasRole("ADMIN")

                    // User service user endpoints
                    .pathMatchers(USER_PROFILE_ENDPOINT).authenticated()

                    // For user ID paths, we need a simpler approach in WebFlux
                    .pathMatchers(USER_ID_ENDPOINT).authenticated()

                    // Mail service endpoints (admin only)
                    .pathMatchers(MAIL_ENDPOINTS).hasRole("ADMIN")

                    // Default policy - deny all
                    .anyExchange().authenticated()
            }
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION).build()
    }

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
