package com.vertyll.projecta.user.domain.dto

import jakarta.validation.constraints.Email

data class UserUpdateDto(
    val firstName: String,
    val lastName: String,

    @field:Email(message = "Email should be valid")
    val email: String,

    val roles: Set<String> = emptySet(),

    val profilePicture: String? = null,

    val phoneNumber: String? = null,

    val address: String? = null
)