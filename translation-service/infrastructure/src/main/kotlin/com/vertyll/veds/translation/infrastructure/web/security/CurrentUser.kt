package com.vertyll.veds.translation.infrastructure.web.security

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.translation.domain.error.TranslationError
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val CLAIM_PARAM = "claim"

    fun idOf(jwt: Jwt?): UUID {
        val token = jwt ?: throw ApiException(TranslationError.NOT_AUTHENTICATED)
        val subject =
            token.subject
                ?: throw ApiException(TranslationError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to "sub"))

        return try {
            UUID.fromString(subject)
        } catch (e: IllegalArgumentException) {
            throw ApiException(
                TranslationError.TOKEN_CLAIM_MISSING,
                mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
            )
        }
    }
}
