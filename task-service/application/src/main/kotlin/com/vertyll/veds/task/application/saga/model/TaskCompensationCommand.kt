package com.vertyll.veds.task.application.saga.model

sealed interface TaskCompensationCommand {
    data class DeleteTask(
        val taskId: String,
    ) : TaskCompensationCommand

    data class LogTaskCompensation(
        val taskId: String,
    ) : TaskCompensationCommand
}
