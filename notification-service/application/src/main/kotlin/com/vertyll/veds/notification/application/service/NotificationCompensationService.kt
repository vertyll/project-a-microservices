package com.vertyll.veds.notification.application.service

import com.vertyll.veds.notification.application.port.inbound.NotificationCompensationUseCase
import com.vertyll.veds.notification.application.port.outbound.UseCaseLogger
import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import java.util.UUID

class NotificationCompensationService(
    private val notificationRepository: NotificationRepository,
    private val logger: UseCaseLogger,
) : NotificationCompensationUseCase {
    override fun compensate(command: NotificationCompensationCommand) {
        when (command) {
            is NotificationCompensationCommand.DeleteNotification -> retire(command.notificationId)
            is NotificationCompensationCommand.LogNotificationCompensation ->
                logger.info("Compensating notification {} - no state change required", command.notificationId)
        }
    }

    private fun retire(notificationId: String) {
        val notification = notificationRepository.findById(UUID.fromString(notificationId))
        if (notification == null) {
            logger.warn("Nothing to compensate: notification {} no longer exists", notificationId)
            return
        }
        notificationRepository.save(notification.retire())
        logger.info("Compensated notification {} - retired", notificationId)
    }
}
