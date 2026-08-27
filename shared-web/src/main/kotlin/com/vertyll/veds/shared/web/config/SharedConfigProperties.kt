package com.vertyll.veds.shared.web.config

import com.vertyll.veds.shared.web.security.KeycloakJwtAuthenticationConverter
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe binding for the `veds.shared.*` configuration namespace
 * loaded from `shared-config.yml` by [SharedConfigEnvironmentPostProcessor].
 *
 * Carries the cross-service Keycloak/identity settings consumed by the
 * security and Keycloak-admin layers (notably [KeycloakJwtAuthenticationConverter]
 * and its reactive counterpart).
 */
@ConfigurationProperties(prefix = "veds.shared")
data class SharedConfigProperties(
    /** Container for Keycloak server, realm, client and cookie settings. */
    val keycloak: KeycloakProperties,
) {
    data class KeycloakProperties(
        val serverUrl: String,
        val realm: String,
        val adminClientId: String,
        val adminClientSecret: String,
        val gatewayClientId: String,
        val gatewayClientSecret: String,
        val rolesClaimPath: String,
        val oauth: OAuthProperties,
        val cookie: CookieProperties,
    ) {
        data class OAuthProperties(
            val redirectUri: String,
            val postLoginRedirectUri: String,
        )

        data class CookieProperties(
            val refreshTokenCookieName: String,
            val httpOnly: Boolean,
            val secure: Boolean,
            val sameSite: String,
            val path: String,
        )
    }
}
