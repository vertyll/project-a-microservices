package com.vertyll.veds.iam.infrastructure.web.security

import com.vertyll.veds.iam.application.dto.AuthenticatedIdentity
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.sharederror.ApiException
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val CLAIM_PARAM = "claim"
    private const val EMAIL_CLAIM = "email"
    private const val GIVEN_NAME_CLAIM = "given_name"
    private const val FAMILY_NAME_CLAIM = "family_name"

    fun keycloakIdOf(jwt: Jwt?): UUID {
        val token = jwt ?: throw ApiException(IamError.NOT_AUTHENTICATED)
        val subject =
            token.subject ?: throw ApiException(IamError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to "sub"))

        return try {
            UUID.fromString(subject)
        } catch (e: IllegalArgumentException) {
            throw ApiException(
                IamError.TOKEN_CLAIM_MISSING,
                mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
            )
        }
    }

    fun emailOf(jwt: Jwt?): String {
        val token = jwt ?: throw ApiException(IamError.NOT_AUTHENTICATED)
        return token.getClaimAsString(EMAIL_CLAIM)
            ?: throw ApiException(IamError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to EMAIL_CLAIM))
    }

    fun identityOf(jwt: Jwt?): AuthenticatedIdentity {
        val token = jwt ?: throw ApiException(IamError.NOT_AUTHENTICATED)

        return AuthenticatedIdentity(
            keycloakId = keycloakIdOf(token),
            email = emailOf(token),
            firstName = token.getClaimAsString(GIVEN_NAME_CLAIM).orEmpty(),
            lastName = token.getClaimAsString(FAMILY_NAME_CLAIM).orEmpty(),
        )
    }
}
