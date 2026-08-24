package com.vertyll.veds.task.domain.model

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaskCommentTest {
    private val author = UUID.randomUUID()
    private val someoneElse = UUID.randomUUID()

    private fun comment() = TaskComment.create(taskId = UUID.randomUUID(), authorId = author, content = "Looks good")

    @Test
    fun `the author may edit`() {
        val edited = comment().editedBy(author, "Looks better")

        assertEquals("Looks better", edited.content)
    }

    @Test
    fun `nobody else may edit, even a manager`() {
        assertFailsWith<IllegalStateException> { comment().editedBy(someoneElse, "Hijacked") }
    }

    @Test
    fun `rejects blank content`() {
        assertFailsWith<IllegalArgumentException> {
            TaskComment.create(taskId = UUID.randomUUID(), authorId = author, content = "   ")
        }
    }

    @Test
    fun `recognises its author`() {
        assertTrue(comment().isAuthoredBy(author))
    }
}
