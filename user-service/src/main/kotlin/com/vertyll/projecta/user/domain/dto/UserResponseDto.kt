package com.vertyll.projecta.user.domain.dto

import com.vertyll.projecta.sharedinfrastructure.role.RoleType

data class UserResponseDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roles: Set<RoleType>,
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
