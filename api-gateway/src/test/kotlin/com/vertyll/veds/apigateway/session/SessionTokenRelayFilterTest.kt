package com.vertyll.veds.apigateway.session

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.test.assertEquals

internal class SessionTokenRelayFilterTest {
    private val sharedConfig =
        SharedKeycloakProperties(
            serverUrl = "http://localhost:9000",
            realm = "veds",
            adminClientId = "veds-service-account",
            adminClientSecret = "secret",
            gatewayClientId = "veds-api-gateway",
            gatewayClientSecret = "secret",
            rolesClaimPath = "realm_access.roles",
            oauth =
                SharedKeycloakProperties.OAuthProperties(
                    redirectUri = "http://localhost:8080/auth/callback",
                    postLoginRedirectUri = "http://localhost:4200/",
                ),
            cookie =
                SharedKeycloakProperties.CookieProperties(
                    refreshTokenCookieName = "KEYCLOAK_REFRESH_TOKEN",
                    httpOnly = true,
                    secure = false,
                    sameSite = "Strict",
                    path = "/",
                ),
        )

    private class RecordingChain : WebFilterChain {
        val authorizationHeaders = mutableListOf<String?>()

        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            authorizationHeaders += exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            return Mono.empty()
        }
    }

    private fun filterWith(store: SessionStore) =
        SessionTokenRelayFilter(
            sessionStore = store,
            sessionCookies = SessionCookies(sharedConfig),
            keycloakTokenClient = mock(KeycloakTokenClient::class.java),
        )

    private fun exchangeWithSession() =
        MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/notifications")
                .cookie(org.springframework.http.HttpCookie(SessionCookies.SESSION_COOKIE, "session-id")),
        )

    private fun session() =
        AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            accessTokenExpiresAt = Instant.now().plusSeconds(300),
            subject = "subject",
            email = "user@example.com",
            roles = listOf("USER"),
        )

    @Test
    fun `a resolved session relays the bearer token and runs the chain exactly once`() {
        val store =
            object : NoOpSessionStore() {
                override fun find(sessionId: String): Mono<AuthSession> = Mono.just(session())
            }
        val chain = RecordingChain()

        filterWith(store).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>("Bearer access-token"), chain.authorizationHeaders)
    }

    @Test
    fun `an unknown session runs the chain exactly once without a bearer token`() {
        val chain = RecordingChain()

        filterWith(NoOpSessionStore()).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>(null), chain.authorizationHeaders)
    }

    private open class NoOpSessionStore : SessionStore {
        override fun create(session: AuthSession): Mono<String> = Mono.empty()

        override fun find(sessionId: String): Mono<AuthSession> = Mono.empty()

        override fun update(
            sessionId: String,
            session: AuthSession,
        ): Mono<Void> = Mono.empty()

        override fun delete(sessionId: String): Mono<Void> = Mono.empty()
    }
}
