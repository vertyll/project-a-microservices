package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.command.ChangeEmailCommand
import com.vertyll.veds.iam.application.command.ChangePasswordCommand
import com.vertyll.veds.iam.application.command.RegisterCommand
import com.vertyll.veds.iam.application.command.ResetPasswordCommand

@Suppress("TooManyFunctions")
interface AuthCommandUseCase {
    fun register(request: RegisterCommand)

    fun activateAccount(token: String)

    fun resendActivationEmail(email: String)

    fun sendPasswordResetRequest(email: String)

    fun resetPassword(
        token: String,
        request: ResetPasswordCommand,
    )

    fun requestEmailChange(
        userId: Long,
        request: ChangeEmailCommand,
    )

    fun confirmEmailChange(token: String)

    fun changePassword(
        userId: Long,
        request: ChangePasswordCommand,
    )

    fun confirmPasswordChange(
        userId: Long,
        code: String,
    )

    fun setNewPassword(
        userId: Long,
        newPassword: String,
    )
}
