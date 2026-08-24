package com.vertyll.veds.iam.infrastructure.web.dto

import com.vertyll.veds.iam.application.command.ChangePasswordCommand
import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest(
    @field:NotBlank(message = "validation.iam.current_password_required")
    val currentPassword: String = "",
) {
    fun toCommand(): ChangePasswordCommand =
        ChangePasswordCommand(
            currentPassword = currentPassword,
        )
}
