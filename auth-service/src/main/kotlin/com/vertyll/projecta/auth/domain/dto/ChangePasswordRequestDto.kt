package com.vertyll.projecta.auth.domain.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequestDto(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String = "",

    @field:NotBlank(message = "New password is required")
    val newPassword: String = ""
)
