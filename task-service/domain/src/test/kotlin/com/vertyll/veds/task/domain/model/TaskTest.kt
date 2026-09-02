package com.vertyll.veds.task.domain.model

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TaskTest {
    private val creator = UUID.randomUUID()
    private val statusId = UUID.randomUUID()

    private fun task() = Task(projectId = UUID.randomUUID(), number = 1, description = "Write the thing", createdBy = creator)

    @Test
    fun `moving to the same status returns the very same instance`() {
        val original = task()
        assertSame(original, original.moveTo(null))

        val withStatus = original.moveTo(statusId)
        assertSame(withStatus, withStatus.moveTo(statusId))
    }

    @Test
    fun `moving to a different status produces a new state`() {
        val original = task()
        val moved = original.moveTo(statusId)

        assertEquals(statusId, moved.statusId)
        assertEquals(original.id, moved.id)
    }

    @Test
    fun `rejects a blank description`() {
        assertFailsWith<IllegalArgumentException> {
            Task(projectId = UUID.randomUUID(), number = 1, description = "  ", createdBy = creator)
        }
    }

    @Test
    fun `rejects negative money and time`() {
        assertFailsWith<IllegalArgumentException> { task().estimateAt(-1) }
        assertFailsWith<IllegalArgumentException> { task().logWork(-1) }
    }

    @Test
    fun `logging work accumulates`() {
        val worked = task().logWork(150).logWork(50)

        assertEquals(200, worked.workedTime)
    }

    @Test
    fun `archiving keeps the record and can be undone`() {
        val original = task()
        val archived = original.archive()

        assertFalse(archived.isActive)
        assertTrue(archived.restore().isActive)
        assertEquals(original.id, archived.restore().id)
    }

    @Test
    fun `dropping a category is idempotent`() {
        val categoryId = UUID.randomUUID()
        val categorised = task().categoriseAs(setOf(categoryId))

        assertEquals(emptySet(), categorised.withoutCategory(categoryId).categoryIds)
        assertSame(categorised, categorised.withoutCategory(UUID.randomUUID()))
    }

    @Test
    fun `clearing a status only affects the task that had it`() {
        val moved = task().moveTo(statusId)

        assertEquals(null, moved.withoutStatus(statusId).statusId)
        assertSame(moved, moved.withoutStatus(UUID.randomUUID()))
    }

    @Test
    fun `dropping an attachment is idempotent`() {
        val fileId = UUID.randomUUID()
        val withFile = task().withAttachments(setOf(fileId))

        assertEquals(emptySet(), withFile.withoutAttachment(fileId).attachmentIds)
        assertSame(withFile, withFile.withoutAttachment(UUID.randomUUID()))
    }
}
