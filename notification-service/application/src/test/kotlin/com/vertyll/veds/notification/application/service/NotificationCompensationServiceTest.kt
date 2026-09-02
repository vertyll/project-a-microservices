@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.notification.application.service

import com.vertyll.veds.notification.application.InMemoryNotificationRepository
import com.vertyll.veds.notification.application.SilentLogger
import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationType
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class NotificationCompensationServiceTest {
    private val notifications = InMemoryNotificationRepository()
    private val service = NotificationCompensationService(notifications, SilentLogger)

    private fun givenNotification() =
        Notification(recipientId = Uuid.generateV7().toJavaUuid(), type = NotificationType.PROJECT_INVITATION)
            .also { notifications.given(it) }

    @Test
    fun `a notification from a failed workflow is retired, not deleted`() {
        val raised = givenNotification()

        service.compensate(NotificationCompensationCommand.DeleteNotification(raised.id.toString()))

        val stored = notifications.findById(raised.id)
        assertNotNull(stored)
        assertTrue(!stored.isActive)
    }

    @Test
    fun `retiring the same notification twice is harmless`() {
        val raised = givenNotification()
        val command = NotificationCompensationCommand.DeleteNotification(raised.id.toString())

        service.compensate(command)
        service.compensate(command)

        assertTrue(!notifications.findById(raised.id)!!.isActive)
    }

    @Test
    fun `a notification that no longer exists is not an error`() {
        service.compensate(NotificationCompensationCommand.DeleteNotification(Uuid.generateV7().toString()))

        assertTrue(notifications.stored.isEmpty())
    }

    @Test
    fun `a step with nothing to undo leaves the notifications alone`() {
        val raised = givenNotification()

        service.compensate(NotificationCompensationCommand.LogNotificationCompensation(raised.id.toString()))

        assertTrue(notifications.findById(raised.id)!!.isActive)
    }
}
