package com.vertyll.projecta.user.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailUpdateDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val currentEmail: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val newEmail: String,
)
