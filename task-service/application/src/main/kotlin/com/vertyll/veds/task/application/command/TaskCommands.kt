package com.vertyll.veds.task.application.command

import com.vertyll.veds.task.domain.model.TaskPriority
import java.util.UUID

data class CreateTaskCommand(
    val projectId: UUID,
    val description: String,
    val additionalDescription: String?,
    val priority: TaskPriority,
    val statusId: UUID?,
    val categoryIds: Set<UUID>,
    val assigneeIds: Set<UUID>,
    val priceEstimation: Int,
    val accessRoleId: UUID?,
    val attachmentIds: Set<UUID>,
)

data class UpdateTaskCommand(
    val description: String,
    val additionalDescription: String?,
    val priority: TaskPriority,
    val statusId: UUID?,
    val categoryIds: Set<UUID>,
    val assigneeIds: Set<UUID>,
    val priceEstimation: Int,
    val accessRoleId: UUID?,
    val attachmentIds: Set<UUID>,
)

data class ChangeTaskStatusCommand(
    val statusId: UUID?,
)

data class LogWorkCommand(
    val hundredthsOfHour: Int,
)

data class CreateCommentCommand(
    val content: String,
    val attachmentIds: Set<UUID>,
)

data class UpdateCommentCommand(
    val content: String,
)

data class BatchDeleteTasksCommand(
    val taskIds: Set<UUID>,
)
