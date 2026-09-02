package com.vertyll.veds.task.domain.service

import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPermission
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TaskAccessPolicyTest {
    private val projectId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()
    private val clientId = UUID.randomUUID()
    private val strangerId = UUID.randomUUID()

    private fun project(isActive: Boolean = true) = ProjectRef(projectId = projectId, name = "Apollo", isActive = isActive)

    private fun membership(
        userId: UUID,
        roleCode: String,
    ) = ProjectMembershipRef(projectId = projectId, userId = userId, roleCode = roleCode)

    private fun task(
        isActive: Boolean = true,
        accessRoleId: UUID? = null,
        createdBy: UUID = managerId,
        assignees: Set<UUID> = emptySet(),
    ) = Task(
        projectId = projectId,
        number = 1,
        description = "Write the thing",
        createdBy = createdBy,
        isActive = isActive,
        accessRoleId = accessRoleId,
        assigneeIds = assignees,
    )

    @Nested
    inner class RoleMapping {
        @Test
        fun `a manager may do everything`() {
            val member = membership(managerId, "MANAGER")
            TaskPermission.entries.forEach { permission ->
                assertTrue(
                    TaskAccessPolicy.evaluate(project(), member, permission).isPermitted,
                    "manager should hold $permission",
                )
            }
        }

        @Test
        fun `a client may view and comment but not manage`() {
            val member = membership(clientId, "CLIENT")

            assertTrue(TaskAccessPolicy.evaluate(project(), member, TaskPermission.VIEW_TASKS).isPermitted)
            assertTrue(TaskAccessPolicy.evaluate(project(), member, TaskPermission.COMMENT).isPermitted)
            assertFalse(TaskAccessPolicy.evaluate(project(), member, TaskPermission.MANAGE_TASKS).isPermitted)
        }

        @Test
        fun `an unknown role code grants nothing`() {
            val member = membership(clientId, "AUDITOR")

            TaskPermission.entries.forEach { permission ->
                assertFalse(TaskAccessPolicy.evaluate(project(), member, permission).isPermitted)
            }
        }

        @Test
        fun `a non-member gets nothing, even on an active project`() {
            TaskPermission.entries.forEach { permission ->
                assertFalse(TaskAccessPolicy.evaluate(project(), null, permission).isPermitted)
            }
        }
    }

    @Nested
    inner class ResourceState {
        @Test
        fun `an archived project freezes writes for every role`() {
            val member = membership(managerId, "MANAGER")
            val decision = TaskAccessPolicy.evaluate(project(isActive = false), member, TaskPermission.MANAGE_TASKS)

            assertIs<AccessDecision.Deny>(decision)
            assertEquals(TaskError.PROJECT_ARCHIVED, decision.reason)
        }

        @Test
        fun `an archived project still allows reading`() {
            val member = membership(managerId, "MANAGER")

            assertTrue(
                TaskAccessPolicy
                    .evaluate(project(isActive = false), member, TaskPermission.VIEW_TASKS)
                    .isPermitted,
            )
        }

        @Test
        fun `an archived task cannot be changed`() {
            val member = membership(managerId, "MANAGER")
            val decision =
                TaskAccessPolicy.evaluate(project(), member, TaskPermission.MANAGE_TASKS, task(isActive = false))

            assertIs<AccessDecision.Deny>(decision)
            assertEquals(TaskError.TASK_ARCHIVED, decision.reason)
        }
    }

    @Nested
    inner class RestrictedTasks {
        private val restriction = UUID.randomUUID()

        @Test
        fun `an unrestricted task is visible to any member`() {
            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(task(), membership(clientId, "CLIENT"), clientId))
        }

        @Test
        fun `the author always sees their own restricted task`() {
            val restricted = task(accessRoleId = restriction, createdBy = clientId)

            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(restricted, membership(clientId, "CLIENT"), clientId))
        }

        @Test
        fun `an assignee sees a restricted task they were given`() {
            val restricted = task(accessRoleId = restriction, assignees = setOf(clientId))

            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(restricted, membership(clientId, "CLIENT"), clientId))
        }

        @Test
        fun `an unrelated client does not see a restricted task`() {
            val restricted = task(accessRoleId = restriction)

            assertFalse(TaskAccessPolicy.canSeeRestrictedTask(restricted, membership(clientId, "CLIENT"), clientId))
        }

        @Test
        fun `a non-member does not see a restricted task`() {
            val restricted = task(accessRoleId = restriction)

            assertFalse(TaskAccessPolicy.canSeeRestrictedTask(restricted, null, strangerId))
        }
    }

    @Test
    fun `permissionsOf agrees with evaluate for every role`() {
        listOf("MANAGER", "MEMBER", "CLIENT", "AUDITOR", null).forEach { roleCode ->
            val member = roleCode?.let { membership(clientId, it) }
            val reported = TaskAccessPolicy.permissionsOf(member)
            val evaluated =
                TaskPermission.entries
                    .filter { TaskAccessPolicy.evaluate(project(), member, it).isPermitted }
                    .toSet()

            assertEquals(evaluated, reported, "role $roleCode")
        }
    }
}
