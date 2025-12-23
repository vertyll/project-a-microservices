package com.vertyll.projecta.auth.domain.dto

data class AuthResponseDto(
    val token: String,
    val type: String,
    val expiresIn: Long,
)
