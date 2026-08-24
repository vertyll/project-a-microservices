package com.vertyll.veds.iam.infrastructure.web.dto

import com.vertyll.veds.iam.application.command.ChangeEmailCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ChangeEmailRequest(
    @field:NotBlank(message = "validation.iam.current_password_required")
    val password: String = "",
    @field:NotBlank(message = "validation.iam.new_email_required")
    @field:Email(message = "validation.iam.new_email_invalid")
    val newEmail: String = "",
) {
    fun toCommand(): ChangeEmailCommand =
        ChangeEmailCommand(
            password = password,
            newEmail = newEmail,
        )
}
