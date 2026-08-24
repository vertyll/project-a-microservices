package com.vertyll.veds.notification.infrastructure.web.dto

import com.vertyll.veds.notification.application.command.CreateNotificationCommand
import jakarta.validation.constraints.NotBlank

data class CreateNotificationRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
) {
    fun toCommand(): CreateNotificationCommand =
        CreateNotificationCommand(
            name = name,
            payload = payload,
        )
}
