package com.vertyll.projecta.auth.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterRequestDto(
        @field:NotBlank(message = "First name is required") val firstName: String = "",
        @field:NotBlank(message = "Last name is required") val lastName: String = "",
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Email should be valid")
        val email: String = "",
        @field:NotBlank(message = "Password is required") val password: String = ""
)

data class AuthRequestDto(
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Email should be valid")
        val email: String = "",
        @field:NotBlank(message = "Password is required") val password: String = "",
        val deviceInfo: String? = null,
        val userAgent: String? = null
)

data class AuthResponseDto(
        val token: String,
        val type: String = "Bearer",
        val expiresIn: Long = 900000 // 15 minutes in milliseconds
)

data class ChangePasswordRequestDto(
        @field:NotBlank(message = "Current password is required") val currentPassword: String = "",
        @field:NotBlank(message = "New password is required") val newPassword: String = ""
)

data class ResetPasswordRequestDto(
        @field:NotBlank(message = "New password is required") val newPassword: String = ""
)

data class ChangeEmailRequestDto(
        @field:NotBlank(message = "Current password is required") val password: String = "",
        @field:NotBlank(message = "New email is required")
        @field:Email(message = "New email should be valid")
        val newEmail: String = ""
)
