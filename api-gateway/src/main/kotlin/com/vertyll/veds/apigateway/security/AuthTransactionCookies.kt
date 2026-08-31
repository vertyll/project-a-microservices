package com.vertyll.veds.apigateway.security

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.time.Duration

@Component
internal class AuthTransactionCookies(
    private val sharedConfig: SharedKeycloakProperties,
) {
    companion object {
        const val STATE_COOKIE = "KEYCLOAK_AUTH_STATE"
        const val VERIFIER_COOKIE = "KEYCLOAK_CODE_VERIFIER"
        private const val SAME_SITE_LAX = "Lax"
        private val LIFETIME: Duration = Duration.ofMinutes(10)
    }

    fun issue(
        exchange: ServerWebExchange,
        state: String,
        codeVerifier: String,
    ) {
        exchange.response.addCookie(transactionCookie(STATE_COOKIE, state, LIFETIME))
        exchange.response.addCookie(transactionCookie(VERIFIER_COOKIE, codeVerifier, LIFETIME))
    }

    fun clear(exchange: ServerWebExchange) {
        exchange.response.addCookie(transactionCookie(STATE_COOKIE, "", Duration.ZERO))
        exchange.response.addCookie(transactionCookie(VERIFIER_COOKIE, "", Duration.ZERO))
    }

    fun read(
        exchange: ServerWebExchange,
        name: String,
    ): String? =
        exchange.request.cookies
            .getFirst(name)
            ?.value
            ?.takeIf { it.isNotBlank() }

    private fun transactionCookie(
        name: String,
        value: String,
        maxAge: Duration,
    ): ResponseCookie =
        ResponseCookie
            .from(name, value)
            .httpOnly(true)
            .secure(sharedConfig.cookie.secure)
            .sameSite(SAME_SITE_LAX)
            .path(sharedConfig.cookie.path)
            .maxAge(maxAge)
            .build()
}
