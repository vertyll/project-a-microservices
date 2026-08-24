package com.vertyll.veds.notification.application.port.inbound

import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand

fun interface NotificationCompensationUseCase {
    fun compensate(command: NotificationCompensationCommand)
}
