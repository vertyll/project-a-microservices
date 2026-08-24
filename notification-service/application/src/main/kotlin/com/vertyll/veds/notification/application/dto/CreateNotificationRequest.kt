package com.vertyll.veds.notification.application.dto

import jakarta.validation.constraints.NotBlank

data class CreateNotificationRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
)
