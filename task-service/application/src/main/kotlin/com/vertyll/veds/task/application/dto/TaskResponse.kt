package com.vertyll.veds.task.application.dto

import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPriority
import java.time.Instant
import java.util.UUID

data class TaskResponse(
    val id: UUID,
    val projectId: UUID,
    val description: String,
    val additionalDescription: String?,
    val priority: TaskPriority,
    val priceEstimation: Int,
    val workedTime: Int,
    val statusId: UUID?,
    val categoryIds: Set<UUID>,
    val assigneeIds: Set<UUID>,
    val accessRoleId: UUID?,
    val attachmentIds: Set<UUID>,
    val createdBy: UUID,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
) {
    companion object {
        fun from(task: Task): TaskResponse =
            TaskResponse(
                id = task.id,
                projectId = task.projectId,
                description = task.description,
                additionalDescription = task.additionalDescription,
                priority = task.priority,
                priceEstimation = task.priceEstimation,
                workedTime = task.workedTime,
                statusId = task.statusId,
                categoryIds = task.categoryIds,
                assigneeIds = task.assigneeIds,
                accessRoleId = task.accessRoleId,
                attachmentIds = task.attachmentIds,
                createdBy = task.createdBy,
                isActive = task.isActive,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                version = task.version,
            )
    }
}
