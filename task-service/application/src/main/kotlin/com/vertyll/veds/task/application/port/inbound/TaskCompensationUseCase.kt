package com.vertyll.veds.task.application.port.inbound

import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand

fun interface TaskCompensationUseCase {
    fun compensate(command: TaskCompensationCommand)
}
