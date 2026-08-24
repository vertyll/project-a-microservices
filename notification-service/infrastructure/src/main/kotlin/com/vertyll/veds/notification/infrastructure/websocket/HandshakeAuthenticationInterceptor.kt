package com.vertyll.veds.notification.infrastructure.websocket

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.UUID

/**
 * Authenticates the WebSocket handshake.
 *
 * The browser cannot set an `Authorization` header on a WebSocket handshake, so
 * the token arrives as a query parameter. That is the standard workaround and it
 * is acceptable here for two reasons: the connection is opened over TLS, and the
 * token is short-lived. It is still worse than a header — it can land in access
 * logs — which is why the gateway is the only thing that should ever see it.
 *
 * The subject is resolved once, at handshake, and stored in the session
 * attributes. Re-validating per message would buy nothing: the session already
 * exists.
 */
@Component
internal class HandshakeAuthenticationInterceptor(
    private val jwtDecoder: JwtDecoder,
) : HandshakeInterceptor {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val USER_ID_ATTRIBUTE = "veds.userId"
        private const val TOKEN_PARAM = "token"
    }

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val token = tokenOf(request)
        if (token == null) {
            logger.debug("Rejecting handshake: no token supplied")
            return false
        }

        return try {
            val jwt = jwtDecoder.decode(token)
            val subject = jwt.subject ?: return false
            attributes[USER_ID_ATTRIBUTE] = UUID.fromString(subject)
            true
        } catch (e: Exception) {
            // Rejected, not thrown: a bad token is a client problem, and the
            // handshake simply fails with 401.
            logger.debug("Rejecting handshake: {}", e.message)
            false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) = Unit

    private fun tokenOf(request: ServerHttpRequest): String? {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest ?: return null
        return servletRequest.getParameter(TOKEN_PARAM)?.takeIf { it.isNotBlank() }
    }
}
