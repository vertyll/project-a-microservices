package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ConfirmPasswordChangeRequest(
    @field:NotBlank(message = "validation.iam.new_password_required")
    @field:Size(min = 8, max = 128, message = "validation.iam.password_length")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "validation.iam.password_complexity",
    )
    val newPassword: String = "",
)
