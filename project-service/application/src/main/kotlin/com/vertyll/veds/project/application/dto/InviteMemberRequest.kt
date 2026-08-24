package com.vertyll.veds.project.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class InviteMemberRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String = "",
    val roleId: UUID? = null,
)
