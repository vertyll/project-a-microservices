package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.SilentLogger
import com.vertyll.veds.task.application.categoryRef
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.projectRef
import com.vertyll.veds.task.application.statusRef
import com.vertyll.veds.task.application.task
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * task-service answers "who may see this board" and "what does this label say" without calling
 * project-service, from a local copy kept current by events. The copy is only as good as this
 * handler: a membership it fails to drop is access that outlives its revocation, and a category it
 * fails to remove is a chip on a card pointing at something that no longer exists.
 *
 * Delivery is at-least-once, so every one of these has to survive being applied twice.
 */
internal class ProjectProjectionServiceTest {
    private val directory = InMemoryProjectDirectory()
    private val tasks = InMemoryTaskRepository()

    private val service = ProjectProjectionService(directory, tasks, SilentLogger)

    private val projectId = UUID.randomUUID()

    // ── The project itself ──────────────────────────────────────────────

    @Test
    fun `a project change is recorded locally`() {
        service.projectChanged(projectRef(projectId, name = "Apollo"))

        assertEquals("Apollo", directory.findProject(projectId)!!.name)
    }

    @Test
    fun `a later change replaces the earlier copy`() {
        service.projectChanged(projectRef(projectId, name = "Apollo"))
        service.projectChanged(projectRef(projectId, name = "Artemis"))

        assertEquals("Artemis", directory.findProject(projectId)!!.name)
        assertEquals(1, directory.projects.size)
    }

    @Test
    fun `archiving marks the local copy inactive rather than deleting it`() {
        service.projectChanged(projectRef(projectId))

        service.projectArchived(projectId)

        val stored = directory.findProject(projectId)!!
        assertTrue(!stored.isActive, "tasks still reference the project, so the row has to stay")
    }

    /**
     * Events can arrive out of order or for a project this service never heard about. Creating a
     * hollow row from an archival would invent a project with no name.
     */
    @Test
    fun `archiving a project that was never projected is ignored`() {
        service.projectArchived(UUID.randomUUID())

        assertTrue(directory.projects.isEmpty())
    }

    // ── Categories ──────────────────────────────────────────────────────

    @Test
    fun `a category change is recorded locally`() {
        val category = categoryRef(projectId, name = "Bug")

        service.categoryChanged(category)

        assertEquals(listOf(category), directory.findCategories(projectId))
    }

    /**
     * A task keeps its own list of category ids. Dropping the projection alone would leave those
     * ids pointing at nothing, and the board would render a chip it cannot name.
     */
    @Test
    fun `removing a category also strips it from the tasks carrying it`() {
        val category = categoryRef(projectId)
        val other = UUID.randomUUID()
        service.categoryChanged(category)
        val labelled = task(projectId, categoryIds = setOf(category.categoryId, other)).also { tasks.given(it) }

        service.categoryRemoved(category.categoryId)

        assertEquals(setOf(other), tasks.findById(labelled.id)!!.categoryIds)
        assertTrue(directory.findCategories(projectId).isEmpty())
    }

    @Test
    fun `removing a category twice is harmless`() {
        val category = categoryRef(projectId)
        service.categoryChanged(category)
        val labelled = task(projectId, categoryIds = setOf(category.categoryId)).also { tasks.given(it) }

        service.categoryRemoved(category.categoryId)
        service.categoryRemoved(category.categoryId)

        assertTrue(tasks.findById(labelled.id)!!.categoryIds.isEmpty())
    }

    @Test
    fun `tasks without the removed category are left alone`() {
        val category = categoryRef(projectId)
        val untouched = task(projectId).also { tasks.given(it) }

        service.categoryRemoved(category.categoryId)

        assertEquals(untouched, tasks.findById(untouched.id))
    }

    // ── Statuses ────────────────────────────────────────────────────────

    @Test
    fun `a status change is recorded locally`() {
        val status = statusRef(projectId, name = "In progress")

        service.statusChanged(status)

        assertEquals(listOf(status), directory.findStatuses(projectId))
    }

    /** A card whose column was deleted has to fall back to no column, not to a dangling id. */
    @Test
    fun `removing a status clears it from the tasks sitting in it`() {
        val status = statusRef(projectId)
        service.statusChanged(status)
        val sitting = task(projectId, statusId = status.statusId).also { tasks.given(it) }

        service.statusRemoved(status.statusId)

        assertNull(tasks.findById(sitting.id)!!.statusId)
        assertTrue(directory.findStatuses(projectId).isEmpty())
    }

    @Test
    fun `removing a status twice is harmless`() {
        val status = statusRef(projectId)
        val sitting = task(projectId, statusId = status.statusId).also { tasks.given(it) }

        service.statusRemoved(status.statusId)
        service.statusRemoved(status.statusId)

        assertNull(tasks.findById(sitting.id)!!.statusId)
    }

    // ── Memberships ─────────────────────────────────────────────────────

    @Test
    fun `a member joining is recorded locally`() {
        val joined = membership(projectId, roleCode = "MANAGER")

        service.memberJoined(joined)

        assertEquals(joined, directory.findMembership(projectId, joined.userId))
    }

    /** A role change arrives as another join; it must replace the row, not add a second one. */
    @Test
    fun `a role change replaces the existing membership`() {
        val userId = UUID.randomUUID()
        service.memberJoined(membership(projectId, userId, roleCode = "MEMBER"))
        service.memberJoined(membership(projectId, userId, roleCode = "MANAGER"))

        assertEquals("MANAGER", directory.findMembership(projectId, userId)!!.roleCode)
        assertEquals(1, directory.findMemberships(projectId).size)
    }

    /** Until this row is gone the removed member still passes every access check on this board. */
    @Test
    fun `a member being removed loses their local membership`() {
        val joined = membership(projectId)
        service.memberJoined(joined)

        service.memberRemoved(projectId, joined.userId)

        assertNull(directory.findMembership(projectId, joined.userId))
    }

    @Test
    fun `removing a member twice is harmless`() {
        val joined = membership(projectId)
        service.memberJoined(joined)

        service.memberRemoved(projectId, joined.userId)
        service.memberRemoved(projectId, joined.userId)

        assertTrue(directory.findMemberships(projectId).isEmpty())
    }

    @Test
    fun `removing a member leaves the other members of the project`() {
        val leaving = membership(projectId)
        val staying = membership(projectId)
        service.memberJoined(leaving)
        service.memberJoined(staying)

        service.memberRemoved(projectId, leaving.userId)

        assertEquals(listOf(staying), directory.findMemberships(projectId))
    }
}
