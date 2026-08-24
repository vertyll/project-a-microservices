package com.vertyll.veds.iam.infrastructure.web.dto

import com.vertyll.veds.iam.application.command.RegisterCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "validation.iam.first_name_required")
    @field:Size(min = 2, max = 50, message = "validation.iam.first_name_length")
    val firstName: String = "",
    @field:NotBlank(message = "validation.iam.last_name_required")
    @field:Size(min = 2, max = 50, message = "validation.iam.last_name_length")
    val lastName: String = "",
    @field:NotBlank(message = "validation.iam.email_required")
    @field:Email(message = "validation.iam.email_invalid")
    @field:Size(max = 255, message = "validation.iam.email_too_long")
    val email: String = "",
    @field:NotBlank(message = "validation.iam.password_required")
    @field:Size(min = 8, max = 128, message = "validation.iam.password_length")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "validation.iam.password_complexity",
    )
    val password: String = "",
) {
    fun toCommand(): RegisterCommand =
        RegisterCommand(
            firstName = firstName,
            lastName = lastName,
            email = email,
            password = password,
        )
}
