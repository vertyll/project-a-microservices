package com.vertyll.veds.shared.web.security

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Autoconfiguration that provides Keycloak JWT converters for both
 * WebMVC (servlet) and WebFlux (reactive) environments.
 */
@Configuration
@ConditionalOnClass(Jwt::class)
internal class KeycloakSecurityAutoConfiguration {
    /** Servlet-stack JWT → `AbstractAuthenticationToken` converter bean. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun keycloakJwtAuthenticationConverter(sharedConfig: SharedKeycloakProperties): KeycloakJwtAuthenticationConverter =
        KeycloakJwtAuthenticationConverter(sharedConfig)

    /**
     * Answers `@PreAuthorize("@authz.has('…')")`. Registered for every service, so
     * a guard written against a permission behaves the same everywhere; one
     * without a [RolePermissionsSource] grants nothing rather than falling back
     * to the role in the token.
     */
    @Bean("authz")
    @ConditionalOnMissingBean(name = ["authz"])
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun permissionAuthorizer(source: ObjectProvider<RolePermissionsSource>): PermissionAuthorizer =
        PermissionAuthorizer(source.getIfAvailable())

    /** Reactive-stack JWT → `Mono<AbstractAuthenticationToken>` converter bean. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    fun reactiveKeycloakJwtAuthenticationConverter(sharedConfig: SharedKeycloakProperties): ReactiveKeycloakJwtAuthenticationConverter =
        ReactiveKeycloakJwtAuthenticationConverter(sharedConfig)
}
