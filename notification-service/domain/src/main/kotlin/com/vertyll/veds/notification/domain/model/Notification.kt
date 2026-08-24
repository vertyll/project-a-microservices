package com.vertyll.veds.notification.domain.model

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val recipientId: UUID,
    val type: NotificationType,
    val params: Map<String, String> = emptyMap(),
    val projectId: UUID? = null,
    val subjectId: UUID? = null,
    val isRead: Boolean = false,
    val readAt: Instant? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun markRead(at: Instant = Instant.now()): Notification = if (isRead) this else copy(isRead = true, readAt = at)

    fun markUnread(): Notification = if (!isRead) this else copy(isRead = false, readAt = null)

    fun retire(): Notification = copy(isActive = false)

    fun isFor(userId: UUID): Boolean = recipientId == userId

    companion object {
        @Suppress("LongParameterList")
        fun create(
            recipientId: UUID,
            type: NotificationType,
            params: Map<String, String> = emptyMap(),
            projectId: UUID? = null,
            subjectId: UUID? = null,
        ): Notification =
            Notification(
                recipientId = recipientId,
                type = type,
                params = params,
                projectId = projectId,
                subjectId = subjectId,
            )
    }
}
