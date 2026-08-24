package com.vertyll.veds.apigateway.session

import java.time.Instant

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Instant,
    val subject: String,
    val email: String,
    val roles: List<String>,
) {
    fun needsRefreshAt(
        now: Instant,
        skewSeconds: Long,
    ): Boolean = now.plusSeconds(skewSeconds).isAfter(accessTokenExpiresAt)
}
