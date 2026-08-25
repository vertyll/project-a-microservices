package com.vertyll.veds.task.domain.service

import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPermission
import java.util.UUID

object TaskAccessPolicy {
    private const val MANAGER_ROLE_CODE = "MANAGER"

    // An unmapped role code grants nothing, on purpose: a role added in
    // project-service must be mapped here consciously rather than inherited.
    private val ROLE_PERMISSIONS: Map<String, Set<TaskPermission>> =
        mapOf(
            MANAGER_ROLE_CODE to setOf(TaskPermission.VIEW_TASKS, TaskPermission.MANAGE_TASKS, TaskPermission.COMMENT),
            "MEMBER" to setOf(TaskPermission.VIEW_TASKS, TaskPermission.MANAGE_TASKS, TaskPermission.COMMENT),
            "CLIENT" to setOf(TaskPermission.VIEW_TASKS, TaskPermission.COMMENT),
        )

    private val MUTATING = setOf(TaskPermission.MANAGE_TASKS)

    fun evaluate(
        project: ProjectRef,
        membership: ProjectMembershipRef?,
        permission: TaskPermission,
        task: Task? = null,
    ): AccessDecision {
        if (!project.isActive && permission in MUTATING) {
            return AccessDecision.Deny(TaskError.PROJECT_ARCHIVED)
        }
        if (task != null && !task.isActive && permission in MUTATING) {
            return AccessDecision.Deny(TaskError.TASK_ARCHIVED)
        }

        val granted = membership?.let { ROLE_PERMISSIONS[it.roleCode] }.orEmpty()
        return if (permission in granted) {
            AccessDecision.Permit
        } else {
            AccessDecision.Deny(TaskError.TASK_ACCESS_DENIED)
        }
    }

    fun permissionsOf(membership: ProjectMembershipRef?): Set<TaskPermission> = membership?.let { ROLE_PERMISSIONS[it.roleCode] }.orEmpty()

    fun canSeeRestrictedTask(
        task: Task,
        membership: ProjectMembershipRef?,
        userId: UUID,
    ): Boolean {
        task.accessRoleId ?: return true
        if (task.wasCreatedBy(userId) || task.isAssignedTo(userId)) return true
        return membership?.roleCode == MANAGER_ROLE_CODE
    }
}
