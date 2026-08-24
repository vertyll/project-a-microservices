package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.dto.SecuritySettingsResponse
import java.util.UUID

@Suppress("kotlin:S6517")
interface SecurityCommandUseCase {
    fun disableTwoFactor(keycloakId: UUID): SecuritySettingsResponse
}
