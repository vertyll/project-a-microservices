package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.categoryRef
import com.vertyll.veds.task.application.exception.ApiException
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.statusRef
import com.vertyll.veds.task.domain.error.TaskError
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * A task may only point at statuses, categories and people that belong to its own project.
 * Everything it names is defined in project-service, so the check runs against the local
 * projection — which is also what stops one project's id being used to label another's task.
 */
internal class TaskReferenceValidatorTest {
    private val directory = InMemoryProjectDirectory()
    private val validator = TaskReferenceValidator(directory)

    private val projectId = UUID.randomUUID()
    private val otherProjectId = UUID.randomUUID()

    private fun validate(
        statusId: UUID? = null,
        categoryIds: Set<UUID> = emptySet(),
        assigneeIds: Set<UUID> = emptySet(),
    ) = validator.validate(projectId, statusId, categoryIds, assigneeIds)

    @Test
    fun `a task naming nothing passes`() {
        validate()
    }

    @Test
    fun `a status belonging to the project is accepted`() {
        val status = statusRef(projectId).also { directory.saveStatus(it) }

        validate(statusId = status.statusId)
    }

    @Test
    fun `an unknown status is refused`() {
        val error = assertFailsWith<ApiException> { validate(statusId = UUID.randomUUID()) }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
    }

    /** Ids are global, so a real status from a different board must not be usable here. */
    @Test
    fun `a status belonging to another project is refused`() {
        val elsewhere = statusRef(otherProjectId).also { directory.saveStatus(it) }

        val error = assertFailsWith<ApiException> { validate(statusId = elsewhere.statusId) }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
    }

    @Test
    fun `categories belonging to the project are accepted`() {
        val first = categoryRef(projectId).also { directory.saveCategory(it) }
        val second = categoryRef(projectId).also { directory.saveCategory(it) }

        validate(categoryIds = setOf(first.categoryId, second.categoryId))
    }

    /** The message names the offending ids, so the caller can tell which of several was wrong. */
    @Test
    fun `an unknown category is refused and named`() {
        val known = categoryRef(projectId).also { directory.saveCategory(it) }
        val unknown = UUID.randomUUID()

        val error = assertFailsWith<ApiException> { validate(categoryIds = setOf(known.categoryId, unknown)) }

        assertEquals(TaskError.CATEGORY_NOT_IN_PROJECT, error.error)
        assertEquals(listOf(unknown.toString()), error.params["categoryIds"])
    }

    @Test
    fun `a category belonging to another project is refused`() {
        val elsewhere = categoryRef(otherProjectId).also { directory.saveCategory(it) }

        assertFailsWith<ApiException> { validate(categoryIds = setOf(elsewhere.categoryId)) }
    }

    @Test
    fun `members of the project can be assigned`() {
        val member = membership(projectId).also { directory.saveMembership(it) }

        validate(assigneeIds = setOf(member.userId))
    }

    /**
     * Assigning an outsider would put a task on the board of someone who cannot open it, and give
     * them a notification about work they have no access to.
     */
    @Test
    fun `someone who is not a member cannot be assigned`() {
        val outsider = UUID.randomUUID()

        val error = assertFailsWith<ApiException> { validate(assigneeIds = setOf(outsider)) }

        assertEquals(TaskError.ASSIGNEE_NOT_A_MEMBER, error.error)
        assertEquals(listOf(outsider.toString()), error.params["userIds"])
    }

    @Test
    fun `a member of another project cannot be assigned`() {
        val elsewhere = membership(otherProjectId).also { directory.saveMembership(it) }

        assertFailsWith<ApiException> { validate(assigneeIds = setOf(elsewhere.userId)) }
    }

    /** The status is checked first: it is the one a board drag is most likely to get wrong. */
    @Test
    fun `a bad status is reported before a bad assignee`() {
        val error = assertFailsWith<ApiException> { validate(statusId = UUID.randomUUID(), assigneeIds = setOf(UUID.randomUUID())) }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
    }
}
