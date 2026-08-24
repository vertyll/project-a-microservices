package com.vertyll.veds.iam.application.command

import java.util.UUID

data class ChangeEmailCommand(
    val password: String,
    val newEmail: String,
)

data class ChangePasswordCommand(
    val currentPassword: String,
)

data class ConfirmPasswordChangeCommand(
    val newPassword: String,
)

data class RegisterCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
)

data class ResetPasswordCommand(
    val newPassword: String,
    val confirmationCode: String,
)

data class UpdateProfileCommand(
    val firstName: String,
    val lastName: String,
    val avatarFileId: UUID?,
    val phoneNumber: String?,
    val address: String?,
)
