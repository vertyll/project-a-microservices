package com.vertyll.veds.notification.application

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.application.port.outbound.NotificationPushPort
import com.vertyll.veds.notification.application.port.outbound.UseCaseLogger
import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationSearchCriteria
import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.domain.model.PageRequest
import com.vertyll.veds.notification.domain.model.PageResult
import com.vertyll.veds.notification.domain.model.RecipientRef
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import com.vertyll.veds.notification.domain.repository.RecipientDirectoryRepository
import java.util.UUID

internal class InMemoryNotificationRepository : NotificationRepository {
    val stored = linkedMapOf<UUID, Notification>()

    fun given(vararg notifications: Notification) = notifications.forEach { stored[it.id] = it }

    override fun save(notification: Notification) = notification.also { stored[it.id] = it }

    override fun saveAll(notifications: Collection<Notification>) = notifications.map { save(it) }

    override fun findById(id: UUID) = stored[id]

    override fun search(
        criteria: NotificationSearchCriteria,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun countUnread(recipientId: UUID) =
        stored.values.count { it.recipientId == recipientId && !it.isRead && it.isActive }.toLong()

    override fun findAllUnreadBy(recipientId: UUID) = stored.values.filter { it.recipientId == recipientId && !it.isRead }

    override fun findAllBySubjectId(subjectId: UUID) = stored.values.filter { it.subjectId == subjectId }
}

/**
 * Settings are created on demand: a user who never opened the preferences screen still has to
 * receive notifications, so the default is what an absent row means.
 */
internal class InMemorySettingsRepository : NotificationSettingsRepository {
    val stored = linkedMapOf<UUID, NotificationSettings>()

    fun given(vararg settings: NotificationSettings) = settings.forEach { stored[it.userId] = it }

    override fun save(settings: NotificationSettings) = settings.also { stored[it.userId] = it }

    override fun findByUserId(userId: UUID) = stored[userId] ?: NotificationSettings.defaultFor(userId)
}

internal class InMemoryRecipientDirectory : RecipientDirectoryRepository {
    val stored = linkedMapOf<UUID, RecipientRef>()

    fun given(vararg recipients: RecipientRef) = recipients.forEach { stored[it.userId] = it }

    override fun save(recipient: RecipientRef) = recipient.also { stored[it.userId] = it }

    override fun findById(userId: UUID) = stored[userId]

    override fun findByEmail(email: String) = stored.values.firstOrNull { it.email == email }
}

/** The browser's live connection. What matters is who was pushed to, and what count they were told. */
internal class RecordingPush : NotificationPushPort {
    val pushed = mutableListOf<String>()
    val unreadCounts = mutableListOf<Pair<UUID, Long>>()

    override fun push(
        recipientId: UUID,
        notification: NotificationResponse,
    ) {
        pushed += "push($recipientId,${notification.type})"
    }

    override fun pushUnreadCount(
        recipientId: UUID,
        unread: Long,
    ) {
        unreadCounts += recipientId to unread
    }
}

internal class RecordingMailRequests : MailRequestPort {
    val requested = mutableListOf<String>()

    override fun requestMail(
        to: String,
        type: NotificationType,
        params: Map<String, String>,
    ) {
        requested += "$to:${type.name}"
    }
}

internal object SilentLogger : UseCaseLogger {
    override fun debug(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun info(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun warn(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun error(
        message: String,
        vararg args: Any?,
    ) = Unit
}
