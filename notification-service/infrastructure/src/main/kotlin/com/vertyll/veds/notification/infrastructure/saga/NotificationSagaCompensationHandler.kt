package com.vertyll.veds.notification.infrastructure.saga

import com.vertyll.veds.notification.application.port.inbound.NotificationCompensationUseCase
import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.shared.saga.engine.CompensationCommandHandler

internal class NotificationSagaCompensationHandler(
    private val notificationCompensationService: NotificationCompensationUseCase,
) : CompensationCommandHandler<NotificationCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: NotificationCompensationCommand,
    ) = notificationCompensationService.compensate(command)
}
