package com.vertyll.veds.task.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandHandler
import com.vertyll.veds.task.application.port.inbound.TaskCompensationUseCase
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand

internal class TaskSagaCompensationHandler(
    private val taskCompensationService: TaskCompensationUseCase,
) : CompensationCommandHandler<TaskCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: TaskCompensationCommand,
    ) = taskCompensationService.compensate(command)
}
