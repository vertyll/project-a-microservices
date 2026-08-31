package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.InMemoryCommentRepository
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.SilentLogger
import com.vertyll.veds.task.application.task
import com.vertyll.veds.task.domain.model.TaskComment
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A file lives in file-service; task-service only holds its id. When the file is gone those ids are
 * links to nothing, and a card offering a download that always fails is worse than one offering
 * none — so the reference is dropped wherever it appears.
 */
internal class FileProjectionServiceTest {
    private val tasks = InMemoryTaskRepository()
    private val comments = InMemoryCommentRepository()

    private val service = FileProjectionService(tasks, comments, SilentLogger)

    private val projectId = UUID.randomUUID()
    private val deleted = UUID.randomUUID()

    private fun comment(vararg attachmentIds: UUID) =
        TaskComment(
            taskId = UUID.randomUUID(),
            authorId = UUID.randomUUID(),
            content = "See the attachment",
            attachmentIds = attachmentIds.toSet(),
        ).also { comments.given(it) }

    @Test
    fun `a deleted file is dropped from the tasks that attached it`() {
        val kept = UUID.randomUUID()
        val attached = task(projectId, attachmentIds = setOf(deleted, kept)).also { tasks.given(it) }

        service.fileDeleted(deleted)

        assertEquals(setOf(kept), tasks.findById(attached.id)!!.attachmentIds)
    }

    @Test
    fun `a deleted file is dropped from the comments that attached it`() {
        val attached = comment(deleted)

        service.fileDeleted(deleted)

        assertTrue(comments.findById(attached.id)!!.attachmentIds.isEmpty())
    }

    @Test
    fun `the same file is dropped from tasks and comments in one pass`() {
        val attachedTask = task(projectId, attachmentIds = setOf(deleted)).also { tasks.given(it) }
        val attachedComment = comment(deleted)

        service.fileDeleted(deleted)

        assertTrue(tasks.findById(attachedTask.id)!!.attachmentIds.isEmpty())
        assertTrue(comments.findById(attachedComment.id)!!.attachmentIds.isEmpty())
    }

    /** Delivery is at-least-once, so a repeat of the same deletion has to find nothing left to do. */
    @Test
    fun `deleting the same file twice is harmless`() {
        val attached = task(projectId, attachmentIds = setOf(deleted)).also { tasks.given(it) }

        service.fileDeleted(deleted)
        service.fileDeleted(deleted)

        assertTrue(tasks.findById(attached.id)!!.attachmentIds.isEmpty())
    }

    @Test
    fun `a file nothing references is ignored`() {
        val untouched = task(projectId, attachmentIds = setOf(UUID.randomUUID())).also { tasks.given(it) }

        service.fileDeleted(deleted)

        assertEquals(untouched, tasks.findById(untouched.id))
    }
}
