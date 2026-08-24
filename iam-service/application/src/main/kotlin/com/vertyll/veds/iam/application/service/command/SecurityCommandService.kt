package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.dto.SecuritySettingsResponse
import com.vertyll.veds.iam.application.port.inbound.command.SecurityCommandUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import java.util.UUID

class SecurityCommandService(
    private val identityProvider: IdentityProviderPort,
) : SecurityCommandUseCase {
    private companion object {
        const val OTP_CREDENTIAL = "otp"
    }

    override fun disableTwoFactor(keycloakId: UUID): SecuritySettingsResponse {
        identityProvider.removeCredential(keycloakId, OTP_CREDENTIAL)

        val factors = identityProvider.credentialTypes(keycloakId)
        return SecuritySettingsResponse(
            twoFactorEnabled = OTP_CREDENTIAL in factors,
            configuredFactors = factors.sorted(),
        )
    }
}
