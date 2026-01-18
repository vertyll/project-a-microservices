package com.vertyll.projecta.user.domain.dto

import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserCreateDto(
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,
    val roles: Set<String> = setOf(RoleType.USER.value),
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
)
