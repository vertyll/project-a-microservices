@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application

import com.vertyll.veds.task.application.port.outbound.UseCaseLogger
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskComment
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.model.WorkLogEntry
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import com.vertyll.veds.task.domain.repository.WorkLogEntryRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal fun projectRef(
    projectId: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "Apollo",
    isActive: Boolean = true,
    hiddenWorkLogEnabled: Boolean = false,
) = ProjectRef(
    projectId = projectId,
    name = name,
    isActive = isActive,
    hiddenWorkLogEnabled = hiddenWorkLogEnabled,
)

internal fun membership(
    projectId: UUID,
    userId: UUID = Uuid.generateV7().toJavaUuid(),
    roleCode: String = "MEMBER",
) = ProjectMembershipRef(projectId = projectId, userId = userId, roleCode = roleCode)

internal fun categoryRef(
    projectId: UUID,
    categoryId: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "Bug",
) = ProjectCategoryRef(categoryId = categoryId, projectId = projectId, names = mapOf("en" to name), color = "#ff0000")

internal fun statusRef(
    projectId: UUID,
    statusId: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "In progress",
) = ProjectStatusRef(statusId = statusId, projectId = projectId, names = mapOf("en" to name), color = "#00ff00")

internal fun task(
    projectId: UUID,
    number: Int = 1,
    createdBy: UUID = Uuid.generateV7().toJavaUuid(),
    name: String = "Fix the thing",
    statusId: UUID? = null,
    categoryIds: Set<UUID> = emptySet(),
    assigneeIds: Set<UUID> = emptySet(),
    attachmentIds: Set<UUID> = emptySet(),
    accessRoleId: UUID? = null,
    version: Long? = 0L,
) = Task(
    projectId = projectId,
    number = number,
    name = name,
    statusId = statusId,
    categoryIds = categoryIds,
    assigneeIds = assigneeIds,
    attachmentIds = attachmentIds,
    accessRoleId = accessRoleId,
    createdBy = createdBy,
    version = version,
)

internal class InMemoryRolePermissions : RolePermissionsRepository {
    private val stored = linkedMapOf<String, RolePermissionsRef>()

    init {
        stockRole("MANAGER", TaskPermission.entries.toSet())
        stockRole(
            "MEMBER",
            setOf(
                TaskPermission.VIEW_TASKS,
                TaskPermission.MANAGE_TASKS,
                TaskPermission.COMMENT,
                TaskPermission.LOG_WORK,
            ),
        )
        stockRole("CLIENT", setOf(TaskPermission.VIEW_TASKS, TaskPermission.COMMENT))
    }

    fun stockRole(
        name: String,
        granted: Set<TaskPermission>,
    ) = save(RolePermissionsRef(roleName = name, permissions = granted.mapTo(mutableSetOf()) { it.name }))

    override fun save(role: RolePermissionsRef) = role.also { stored[it.roleName] = it }

    override fun findByName(roleName: String) = stored[roleName]

    override fun findAll() = stored.values.toList()

    override fun deleteByName(roleName: String) {
        stored.remove(roleName)
    }
}

internal class InMemoryProjectDirectory : ProjectDirectoryRepository {
    val projects = linkedMapOf<UUID, ProjectRef>()
    val categories = linkedMapOf<UUID, ProjectCategoryRef>()
    val statuses = linkedMapOf<UUID, ProjectStatusRef>()
    val memberships = mutableListOf<ProjectMembershipRef>()

    override fun saveProject(project: ProjectRef) = project.also { projects[it.projectId] = it }

    override fun findProject(projectId: UUID) = projects[projectId]

    override fun saveCategory(category: ProjectCategoryRef) = category.also { categories[it.categoryId] = it }

    override fun removeCategory(categoryId: UUID) {
        categories.remove(categoryId)
    }

    override fun findCategories(projectId: UUID) = categories.values.filter { it.projectId == projectId }

    override fun saveStatus(status: ProjectStatusRef) = status.also { statuses[it.statusId] = it }

    override fun removeStatus(statusId: UUID) {
        statuses.remove(statusId)
    }

    override fun findStatuses(projectId: UUID) = statuses.values.filter { it.projectId == projectId }

    override fun saveMembership(membership: ProjectMembershipRef) =
        membership.also {
            memberships.removeAll { existing -> existing.projectId == it.projectId && existing.userId == it.userId }
            memberships += it
        }

    override fun removeMembership(
        projectId: UUID,
        userId: UUID,
    ) {
        memberships.removeAll { it.projectId == projectId && it.userId == userId }
    }

    override fun findMembership(
        projectId: UUID,
        userId: UUID,
    ) = memberships.firstOrNull { it.projectId == projectId && it.userId == userId }

    override fun findMemberships(projectId: UUID) = memberships.filter { it.projectId == projectId }
}

internal class InMemoryTaskRepository : TaskRepository {
    val stored = linkedMapOf<UUID, Task>()

    fun given(vararg tasks: Task) = tasks.forEach { stored[it.id] = it }

    override fun save(task: Task) = task.also { stored[it.id] = it }

    override fun saveAll(tasks: Collection<Task>) = tasks.map { save(it) }

    override fun highestNumberIn(projectId: UUID) = stored.values.filter { it.projectId == projectId }.maxOfOrNull { it.number } ?: 0

    override fun findById(id: UUID) = stored[id]

    override fun findAllByIds(ids: Collection<UUID>) = ids.mapNotNull { stored[it] }

    override fun search(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun findAllByCategoryId(categoryId: UUID) = stored.values.filter { categoryId in it.categoryIds }

    override fun findAllByStatusId(statusId: UUID) = stored.values.filter { it.statusId == statusId }

    override fun findAllByAttachmentId(attachmentId: UUID) = stored.values.filter { attachmentId in it.attachmentIds }

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}

internal class InMemoryCommentRepository : TaskCommentRepository {
    val stored = linkedMapOf<UUID, TaskComment>()

    fun given(vararg comments: TaskComment) = comments.forEach { stored[it.id] = it }

    override fun save(comment: TaskComment) = comment.also { stored[it.id] = it }

    override fun saveAll(comments: Collection<TaskComment>) = comments.map { save(it) }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByTaskId(taskId: UUID) = stored.values.filter { it.taskId == taskId }

    override fun findAllByAttachmentId(attachmentId: UUID) = stored.values.filter { attachmentId in it.attachmentIds }

    override fun delete(id: UUID) {
        stored.remove(id)
    }

    override fun deleteAllByTaskId(taskId: UUID) {
        stored.values.removeAll { it.taskId == taskId }
    }
}

internal class InMemoryWorkLogRepository : WorkLogEntryRepository {
    val stored = linkedMapOf<UUID, WorkLogEntry>()

    override fun save(entry: WorkLogEntry) = entry.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByTaskId(taskId: UUID) = stored.values.filter { it.taskId == taskId }

    override fun deleteById(id: UUID) {
        stored.remove(id)
    }

    override fun sumMinutesByTaskId(taskId: UUID) = stored.values.filter { it.taskId == taskId }.sumOf { it.minutes }
}

internal object SilentLogger : UseCaseLogger {
    override fun debug(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun info(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun warn(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun error(
        message: String,
        vararg args: Any?,
    ) = Unit
}
