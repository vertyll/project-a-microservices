package com.vertyll.projecta.auth.domain.dto

data class AuthResponseDto(
    val token: String,
    val type: String = "Bearer",
    val expiresIn: Long = 900000 // 15 minutes in milliseconds
)