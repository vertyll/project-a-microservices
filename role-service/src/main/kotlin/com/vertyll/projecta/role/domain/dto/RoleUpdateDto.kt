package com.vertyll.projecta.role.domain.dto

import jakarta.validation.constraints.NotBlank

data class RoleUpdateDto(
    @field:NotBlank(message = "Role name is required")
    val name: String,
    val description: String? = null,
)
