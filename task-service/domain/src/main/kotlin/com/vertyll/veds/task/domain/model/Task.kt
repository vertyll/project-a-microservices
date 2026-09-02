package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.util.UUID

data class Task(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val number: Int,
    val description: String,
    val additionalDescription: String? = null,
    val priceEstimation: Int = 0,
    val workedTime: Int = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val statusId: UUID? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val assigneeIds: Set<UUID> = emptySet(),
    val accessRoleId: UUID? = null,
    val attachmentIds: Set<UUID> = emptySet(),
    val createdBy: UUID,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(description.isNotBlank()) { "task description must not be blank" }
        require(priceEstimation >= 0) { "price estimation must not be negative" }
        require(workedTime >= 0) { "worked time must not be negative" }
    }

    fun describe(
        newDescription: String,
        newAdditionalDescription: String?,
    ): Task =
        copy(
            description = newDescription,
            additionalDescription = newAdditionalDescription,
            updatedAt = Instant.now(),
        )

    fun reprioritise(newPriority: TaskPriority): Task = copy(priority = newPriority, updatedAt = Instant.now())

    fun moveTo(newStatusId: UUID?): Task = if (statusId == newStatusId) this else copy(statusId = newStatusId, updatedAt = Instant.now())

    fun categoriseAs(newCategoryIds: Set<UUID>): Task = copy(categoryIds = newCategoryIds, updatedAt = Instant.now())

    fun assignTo(newAssigneeIds: Set<UUID>): Task = copy(assigneeIds = newAssigneeIds, updatedAt = Instant.now())

    fun restrictTo(roleId: UUID?): Task = copy(accessRoleId = roleId, updatedAt = Instant.now())

    fun withAttachments(newAttachmentIds: Set<UUID>): Task = copy(attachmentIds = newAttachmentIds, updatedAt = Instant.now())

    fun withoutAttachment(attachmentId: UUID): Task =
        if (attachmentId in attachmentIds) {
            copy(attachmentIds = attachmentIds - attachmentId, updatedAt = Instant.now())
        } else {
            this
        }

    fun logWork(additionalHundredths: Int): Task {
        require(additionalHundredths >= 0) { "logged work must not be negative" }
        return copy(workedTime = workedTime + additionalHundredths, updatedAt = Instant.now())
    }

    fun estimateAt(hundredths: Int): Task {
        require(hundredths >= 0) { "price estimation must not be negative" }
        return copy(priceEstimation = hundredths, updatedAt = Instant.now())
    }

    fun archive(): Task = copy(isActive = false, updatedAt = Instant.now())

    fun restore(): Task = copy(isActive = true, updatedAt = Instant.now())

    fun isAssignedTo(userId: UUID): Boolean = userId in assigneeIds

    fun wasCreatedBy(userId: UUID): Boolean = createdBy == userId

    fun withoutCategory(categoryId: UUID): Task =
        if (categoryId in categoryIds) {
            copy(categoryIds = categoryIds - categoryId, updatedAt = Instant.now())
        } else {
            this
        }

    fun withoutStatus(removedStatusId: UUID): Task =
        if (statusId == removedStatusId) copy(statusId = null, updatedAt = Instant.now()) else this

    companion object {
        @Suppress("LongParameterList")
        fun create(
            projectId: UUID,
            number: Int,
            description: String,
            additionalDescription: String?,
            priority: TaskPriority,
            statusId: UUID?,
            categoryIds: Set<UUID>,
            assigneeIds: Set<UUID>,
            createdBy: UUID,
            priceEstimation: Int = 0,
            accessRoleId: UUID? = null,
            attachmentIds: Set<UUID> = emptySet(),
        ): Task =
            Task(
                projectId = projectId,
                number = number,
                description = description,
                additionalDescription = additionalDescription,
                priority = priority,
                statusId = statusId,
                categoryIds = categoryIds,
                assigneeIds = assigneeIds,
                createdBy = createdBy,
                priceEstimation = priceEstimation,
                accessRoleId = accessRoleId,
                attachmentIds = attachmentIds,
            )
    }
}
