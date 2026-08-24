package com.vertyll.veds.notification.application.service

import com.vertyll.veds.notification.application.port.inbound.NotificationCompensationUseCase
import com.vertyll.veds.notification.application.port.outbound.UseCaseLogger
import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.domain.repository.NotificationRepository

class NotificationCompensationService(
    private val notificationRepository: NotificationRepository,
    private val logger: UseCaseLogger,
) : NotificationCompensationUseCase {
    override fun compensate(command: NotificationCompensationCommand) {
        when (command) {
            is NotificationCompensationCommand.DeleteNotification -> deleteNotification(command.notificationId)
            is NotificationCompensationCommand.LogNotificationCompensation -> logCompensation(command.notificationId)
        }
    }

    private fun deleteNotification(notificationId: String) {
        logger.info("Compensating PersistNotification — deleting notification {}", notificationId)
        notificationRepository.findById(notificationId)?.let { notificationRepository.deleteById(it.id) }
    }

    private fun logCompensation(notificationId: String) {
        logger.info(
            "Compensating PublishNotificationEvent for notification {} — no externally-observable rollback possible",
            notificationId,
        )
    }
}
