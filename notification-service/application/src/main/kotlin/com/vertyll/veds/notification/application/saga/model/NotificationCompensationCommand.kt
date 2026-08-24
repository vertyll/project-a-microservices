package com.vertyll.veds.notification.application.saga.model

sealed interface NotificationCompensationCommand {
    data class DeleteNotification(
        val notificationId: String,
    ) : NotificationCompensationCommand

    data class LogNotificationCompensation(
        val notificationId: String,
    ) : NotificationCompensationCommand
}
