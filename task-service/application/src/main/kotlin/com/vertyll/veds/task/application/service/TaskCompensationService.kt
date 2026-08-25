package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.port.inbound.TaskCompensationUseCase
import com.vertyll.veds.task.application.port.outbound.UseCaseLogger
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.domain.repository.TaskRepository
import java.util.UUID

class TaskCompensationService(
    private val taskRepository: TaskRepository,
    private val logger: UseCaseLogger,
) : TaskCompensationUseCase {
    override fun compensate(command: TaskCompensationCommand) {
        when (command) {
            is TaskCompensationCommand.DeleteTask -> deleteTask(command.taskId)
            is TaskCompensationCommand.LogTaskCompensation ->
                logger.info("Compensating task {} - no state change required", command.taskId)
        }
    }

    private fun deleteTask(taskId: String) {
        val id = UUID.fromString(taskId)
        if (taskRepository.findById(id) == null) {
            logger.warn("Nothing to compensate: task {} no longer exists", taskId)
            return
        }
        taskRepository.delete(id)
        logger.info("Compensated task creation - deleted {}", taskId)
    }
}
