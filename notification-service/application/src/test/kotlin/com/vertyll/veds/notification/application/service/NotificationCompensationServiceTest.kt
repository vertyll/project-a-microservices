package com.vertyll.veds.notification.application.service

import com.vertyll.veds.notification.application.InMemoryNotificationRepository
import com.vertyll.veds.notification.application.SilentLogger
import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationType
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A notification raised by a workflow that then failed is a message about something that never
 * happened. Compensation retires it rather than deleting it, so the row stays for anyone auditing
 * what the user was shown and when it was withdrawn.
 */
internal class NotificationCompensationServiceTest {
    private val notifications = InMemoryNotificationRepository()
    private val service = NotificationCompensationService(notifications, SilentLogger)

    private fun givenNotification() =
        Notification(recipientId = UUID.randomUUID(), type = NotificationType.PROJECT_INVITATION)
            .also { notifications.given(it) }

    @Test
    fun `a notification from a failed workflow is retired, not deleted`() {
        val raised = givenNotification()

        service.compensate(NotificationCompensationCommand.DeleteNotification(raised.id.toString()))

        val stored = notifications.findById(raised.id)
        assertNotNull(stored)
        assertTrue(!stored.isActive)
    }

    /** Delivery is at-least-once, so the second copy has to find the work already done. */
    @Test
    fun `retiring the same notification twice is harmless`() {
        val raised = givenNotification()
        val command = NotificationCompensationCommand.DeleteNotification(raised.id.toString())

        service.compensate(command)
        service.compensate(command)

        assertTrue(!notifications.findById(raised.id)!!.isActive)
    }

    /** The state this undo exists to reverse is already gone — the outcome that was wanted. */
    @Test
    fun `a notification that no longer exists is not an error`() {
        service.compensate(NotificationCompensationCommand.DeleteNotification(UUID.randomUUID().toString()))

        assertTrue(notifications.stored.isEmpty())
    }

    @Test
    fun `a step with nothing to undo leaves the notifications alone`() {
        val raised = givenNotification()

        service.compensate(NotificationCompensationCommand.LogNotificationCompensation(raised.id.toString()))

        assertTrue(notifications.findById(raised.id)!!.isActive)
    }
}
