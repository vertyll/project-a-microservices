package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.command.ChangeEmailCommand
import com.vertyll.veds.iam.application.command.ChangePasswordCommand
import com.vertyll.veds.iam.application.command.RegisterCommand
import com.vertyll.veds.iam.application.command.ResetPasswordCommand
import java.util.UUID

interface AuthUseCase {
    fun register(request: RegisterCommand)

    fun activateAccount(token: String)

    fun resendActivationEmail(email: String)

    fun sendPasswordResetRequest(email: String)

    fun resetPassword(
        token: String,
        request: ResetPasswordCommand,
    )

    fun requestEmailChange(
        email: String,
        request: ChangeEmailCommand,
    )

    fun confirmEmailChange(token: String)

    fun changePassword(
        email: String,
        request: ChangePasswordCommand,
    )

    fun confirmPasswordChange(
        token: String,
        newPassword: String,
    )

    fun setNewPassword(
        tokenId: Long,
        request: ResetPasswordCommand,
    )

    fun getUserPermissions(keycloakId: UUID): List<String>
}
