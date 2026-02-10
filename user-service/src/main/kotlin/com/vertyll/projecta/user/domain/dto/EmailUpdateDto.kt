package com.vertyll.projecta.user.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailUpdateDto(
    // Preferowane w API: aktualizacja po id + version
    val userId: Long? = null,
    val version: Long? = null,
    // Wsparcie wsteczne dla istniejących eventów/wywołań: aktualizacja po emailu
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val currentEmail: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val newEmail: String,
)
