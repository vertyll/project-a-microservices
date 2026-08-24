package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.BatchDeleteTasksCommand
import com.vertyll.veds.task.application.command.ChangeTaskStatusCommand
import com.vertyll.veds.task.application.command.CreateCommentCommand
import com.vertyll.veds.task.application.command.CreateTaskCommand
import com.vertyll.veds.task.application.command.LogWorkCommand
import com.vertyll.veds.task.application.command.UpdateCommentCommand
import com.vertyll.veds.task.application.command.UpdateTaskCommand
import com.vertyll.veds.task.domain.model.TaskPriority
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class CreateTaskRequest(
    @field:NotBlank(message = "validation.task.description_required")
    val description: String = "",
    val additionalDescription: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val statusId: UUID? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val assigneeIds: Set<UUID> = emptySet(),
    @field:Min(value = 0, message = "validation.task.estimation_negative")
    val priceEstimation: Int = 0,
    val accessRoleId: UUID? = null,
    val attachmentIds: Set<UUID> = emptySet(),
) {
    fun toCommand(projectId: UUID): CreateTaskCommand =
        CreateTaskCommand(
            projectId = projectId,
            description = description,
            additionalDescription = additionalDescription,
            priority = priority,
            statusId = statusId,
            categoryIds = categoryIds,
            assigneeIds = assigneeIds,
            priceEstimation = priceEstimation,
            accessRoleId = accessRoleId,
            attachmentIds = attachmentIds,
        )
}

data class UpdateTaskRequest(
    @field:NotBlank(message = "validation.task.description_required")
    val description: String = "",
    val additionalDescription: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val statusId: UUID? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val assigneeIds: Set<UUID> = emptySet(),
    @field:Min(value = 0, message = "validation.task.estimation_negative")
    val priceEstimation: Int = 0,
    val accessRoleId: UUID? = null,
    val attachmentIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): UpdateTaskCommand =
        UpdateTaskCommand(
            description = description,
            additionalDescription = additionalDescription,
            priority = priority,
            statusId = statusId,
            categoryIds = categoryIds,
            assigneeIds = assigneeIds,
            priceEstimation = priceEstimation,
            accessRoleId = accessRoleId,
            attachmentIds = attachmentIds,
        )
}

data class ChangeTaskStatusRequest(
    val statusId: UUID? = null,
) {
    fun toCommand(): ChangeTaskStatusCommand = ChangeTaskStatusCommand(statusId = statusId)
}

data class LogWorkRequest(
    @field:Min(value = 0, message = "validation.task.worked_time_negative")
    val hundredthsOfHour: Int = 0,
) {
    fun toCommand(): LogWorkCommand = LogWorkCommand(hundredthsOfHour = hundredthsOfHour)
}

data class BatchDeleteTasksRequest(
    @field:NotEmpty(message = "validation.task.batch_empty")
    val taskIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): BatchDeleteTasksCommand = BatchDeleteTasksCommand(taskIds = taskIds)
}

data class CreateCommentRequest(
    @field:NotBlank(message = "validation.task.comment_content_required")
    val content: String = "",
    val attachmentIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): CreateCommentCommand = CreateCommentCommand(content = content, attachmentIds = attachmentIds)
}

data class UpdateCommentRequest(
    @field:NotBlank(message = "validation.task.comment_content_required")
    val content: String = "",
) {
    fun toCommand(): UpdateCommentCommand = UpdateCommentCommand(content = content)
}
