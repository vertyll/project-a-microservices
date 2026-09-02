package com.vertyll.veds.apigateway.session

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
internal class SessionTokenRelayFilter(
    private val sessionStore: SessionStore,
    private val sessionCookies: SessionCookies,
    private val keycloakTokenClient: KeycloakTokenClient,
) : WebFilter,
    Ordered {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private val REFRESH_LOCK_TTL: Duration = Duration.ofSeconds(10)
        private val REFRESH_WAIT_INTERVAL: Duration = Duration.ofMillis(150)
        private const val REFRESH_WAIT_ATTEMPTS = 20
        private const val REFRESH_SKEW_SECONDS = 30L
        private const val BEARER_PREFIX = "Bearer "

        private val EXCLUDED_PREFIXES = listOf("/auth/authorize", "/auth/callback", "/auth/logout", "/auth/session")
    }

    // Must stay a WebFilter at this order, not a Gateway GlobalFilter: those run
    // inside the routing handler, after Spring Security, so a token injected there
    // arrives too late and every request is rejected as anonymous.
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val path = exchange.request.path.value()
        if (EXCLUDED_PREFIXES.any { path.startsWith(it) }) return chain.filter(exchange)

        if (exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) != null) return chain.filter(exchange)

        val sessionId = sessionCookies.read(exchange) ?: return chain.filter(exchange)

        return sessionStore
            .find(sessionId)
            .flatMap { session -> freshen(sessionId, session) }
            .map { session -> withBearer(exchange, session.accessToken) }
            .defaultIfEmpty(exchange)
            .flatMap { relayed -> chain.filter(relayed) }
    }

    private fun freshen(
        sessionId: String,
        session: AuthSession,
    ): Mono<AuthSession> {
        if (!session.needsRefreshAt(Instant.now(), REFRESH_SKEW_SECONDS)) return Mono.just(session)

        return sessionStore
            .claimRefresh(sessionId, REFRESH_LOCK_TTL)
            .flatMap { claimed ->
                if (claimed) refreshNow(sessionId, session) else awaitRefreshedElsewhere(sessionId, session)
            }
    }

    private fun refreshNow(
        sessionId: String,
        session: AuthSession,
    ): Mono<AuthSession> =
        keycloakTokenClient
            .refresh(session.refreshToken)
            .flatMap { refreshed -> sessionStore.update(sessionId, refreshed).thenReturn(refreshed) }
            .onErrorResume { ex -> sessionRefreshedElsewhere(sessionId, session, ex) }

    private fun awaitRefreshedElsewhere(
        sessionId: String,
        stale: AuthSession,
    ): Mono<AuthSession> =
        Mono
            .defer { sessionStore.find(sessionId) }
            .filter { current -> current.refreshToken != stale.refreshToken }
            .repeatWhenEmpty(REFRESH_WAIT_ATTEMPTS) { attempts -> attempts.delayElements(REFRESH_WAIT_INTERVAL) }
            .onErrorReturn(stale)
            .defaultIfEmpty(stale)

    private fun sessionRefreshedElsewhere(
        sessionId: String,
        stale: AuthSession,
        failure: Throwable,
    ): Mono<AuthSession> =
        sessionStore
            .find(sessionId)
            .filter { current -> current.refreshToken != stale.refreshToken }
            .switchIfEmpty(
                Mono.defer {
                    logger.debug("Refresh failed for session, dropping it: {}", failure.message)
                    sessionStore.delete(sessionId).then(Mono.empty<AuthSession>())
                },
            )

    private fun withBearer(
        exchange: ServerWebExchange,
        accessToken: String,
    ): ServerWebExchange =
        exchange
            .mutate()
            .request { it.header(HttpHeaders.AUTHORIZATION, "$BEARER_PREFIX$accessToken") }
            .build()
}
