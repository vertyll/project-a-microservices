package com.vertyll.veds.apigateway.session

import reactor.core.publisher.Mono
import java.time.Duration

interface SessionStore {
    fun create(session: AuthSession): Mono<String>

    fun find(sessionId: String): Mono<AuthSession>

    fun update(
        sessionId: String,
        session: AuthSession,
    ): Mono<Void>

    fun delete(sessionId: String): Mono<Void>

    /**
     * Claims the exclusive right to refresh this session's tokens, expiring after [ttl].
     *
     * Keycloak rotates refresh tokens and revokes the whole session when a spent one is
     * replayed, so two requests refreshing the same session would log the user out. Only the
     * caller that receives `true` may call the token endpoint; the others wait for the result.
     */
    fun claimRefresh(
        sessionId: String,
        ttl: Duration,
    ): Mono<Boolean>
}
