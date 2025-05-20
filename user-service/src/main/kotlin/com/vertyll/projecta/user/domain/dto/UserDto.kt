package com.vertyll.projecta.user.domain.dto

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
    
    val roles: Set<String> = setOf("USER"),
    
    val profilePicture: String? = null,
    
    val phoneNumber: String? = null,
    
    val address: String? = null
)

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

data class UserResponseDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roles: Set<String>,
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class EmailUpdateDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String
)

data class UserRoleUpdateDto(
    val roles: Set<String>
) 