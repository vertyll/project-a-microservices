package com.vertyll.veds.apigateway.session

import com.vertyll.veds.shared.web.config.SharedConfigProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.time.Duration

@Component
internal class SessionCookies(
    private val sharedConfig: SharedConfigProperties,
) {
    companion object {
        const val SESSION_COOKIE = "VEDS_SESSION"
        private const val SAME_SITE_STRICT = "Strict"
        private val LIFETIME: Duration = Duration.ofDays(7)
    }

    fun issue(
        exchange: ServerWebExchange,
        sessionId: String,
    ) = exchange.response.addCookie(sessionCookie(sessionId, LIFETIME))

    fun clear(exchange: ServerWebExchange) = exchange.response.addCookie(sessionCookie("", Duration.ZERO))

    fun read(exchange: ServerWebExchange): String? =
        exchange.request.cookies
            .getFirst(SESSION_COOKIE)
            ?.value
            ?.takeIf { it.isNotBlank() }

    private fun sessionCookie(
        value: String,
        maxAge: Duration,
    ): ResponseCookie =
        ResponseCookie
            .from(SESSION_COOKIE, value)
            .httpOnly(true)
            .secure(sharedConfig.keycloak.cookie.secure)
            .sameSite(SAME_SITE_STRICT)
            .path(sharedConfig.keycloak.cookie.path)
            .maxAge(maxAge)
            .build()
}
