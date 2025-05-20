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

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }  // Disable CSRF for API Gateway
            .formLogin { it.disable() }  // Disable default login form
            .httpBasic { it.disable() }  // Disable HTTP Basic Auth
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints that don't require authentication
                    .pathMatchers("/api/v1/auth/register", "/api/v1/auth/authenticate", 
                                  "/api/v1/auth/refresh-token", "/api/v1/auth/activate",
                                  "/api/v1/auth/reset-password-request", "/api/v1/auth/confirm-reset-password",
                                  "/api/v1/auth/resend-activation").permitAll()
                    
                    // Swagger docs
                    .pathMatchers("/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                    
                    // Health and metrics endpoints
                    .pathMatchers("/actuator/**").permitAll()
                    
                    // Auth service - some endpoints need authentication
                    .pathMatchers("/api/v1/auth/me", "/api/v1/auth/logout", 
                                 "/api/v1/auth/change-password-request", "/api/v1/auth/change-email-request",
                                 "/api/v1/auth/confirm-email-change", "/api/v1/auth/confirm-password-change",
                                 "/api/v1/auth/set-new-password", "/api/v1/auth/sessions/**").authenticated()
                    
                    // Role service admin endpoints
                    .pathMatchers("/api/v1/roles/admin/**").hasRole("ADMIN")
                    
                    // Role service regular endpoints
                    .pathMatchers("/api/v1/roles/**").authenticated()
                    
                    // User service admin endpoints
                    .pathMatchers("/api/v1/users/admin/**").hasRole("ADMIN")
                    
                    // User service user endpoints
                    .pathMatchers("/api/v1/users/me").authenticated()
                    
                    // For user ID paths, we need a simpler approach in WebFlux
                    .pathMatchers("/api/v1/users/{id}").authenticated()
                    
                    // Mail service endpoints (admin only)
                    .pathMatchers("/api/v1/mail/**").hasRole("ADMIN")
                    
                    // Default policy - deny all
                    .anyExchange().authenticated()
            }
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
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
