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

    fun claimRefresh(
        sessionId: String,
        ttl: Duration,
    ): Mono<Boolean>
}
