package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.port.inbound.TaskCompensationUseCase
import com.vertyll.veds.task.application.port.outbound.UseCaseLogger
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.domain.repository.TaskRepository

class TaskCompensationService(
    private val taskRepository: TaskRepository,
    private val logger: UseCaseLogger,
) : TaskCompensationUseCase {
    override fun compensate(command: TaskCompensationCommand) {
        when (command) {
            is TaskCompensationCommand.DeleteTask -> deleteTask(command.taskId)
            is TaskCompensationCommand.LogTaskCompensation -> logCompensation(command.taskId)
        }
    }

    private fun deleteTask(taskId: String) {
        logger.info("Compensating PersistTask — deleting task {}", taskId)
        taskRepository.findById(taskId)?.let { taskRepository.deleteById(it.id) }
    }

    private fun logCompensation(taskId: String) {
        logger.info(
            "Compensating PublishTaskEvent for task {} — no externally-observable rollback possible",
            taskId,
        )
    }
}
