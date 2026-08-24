package com.vertyll.veds.notification.infrastructure.web.dto

import com.vertyll.veds.notification.application.command.MarkReadCommand
import com.vertyll.veds.notification.application.command.UpdateSettingsCommand
import com.vertyll.veds.notification.domain.model.NotificationType
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class MarkReadRequest(
    @field:NotEmpty(message = "validation.notification.ids_required")
    val notificationIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): MarkReadCommand = MarkReadCommand(notificationIds = notificationIds)
}

data class UpdateSettingsRequest(
    val mutedTypes: Set<NotificationType> = emptySet(),
    val emailEnabledTypes: Set<NotificationType> = emptySet(),
) {
    fun toCommand(): UpdateSettingsCommand = UpdateSettingsCommand(mutedTypes = mutedTypes, emailEnabledTypes = emailEnabledTypes)
}
