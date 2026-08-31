package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.SilentLogger
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.application.task
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Undoing a task creation happens after the transaction that made it has already committed, and the
 * command may be delivered more than once — so it has to be safe to apply to a task that is already
 * gone.
 */
internal class TaskCompensationServiceTest {
    private val tasks = InMemoryTaskRepository()
    private val service = TaskCompensationService(tasks, SilentLogger)

    @Test
    fun `a task created by a failed workflow is deleted`() {
        val created = task(UUID.randomUUID()).also { tasks.given(it) }

        service.compensate(TaskCompensationCommand.DeleteTask(created.id.toString()))

        assertNull(tasks.findById(created.id))
    }

    @Test
    fun `deleting the same task twice is harmless`() {
        val created = task(UUID.randomUUID()).also { tasks.given(it) }
        val command = TaskCompensationCommand.DeleteTask(created.id.toString())

        service.compensate(command)
        service.compensate(command)

        assertTrue(tasks.stored.isEmpty())
    }

    /** The state this undo exists to reverse is already gone — the outcome that was wanted. */
    @Test
    fun `a task that no longer exists is not an error`() {
        service.compensate(TaskCompensationCommand.DeleteTask(UUID.randomUUID().toString()))

        assertTrue(tasks.stored.isEmpty())
    }

    /**
     * Some steps commit nothing to undo, but the saga still records that compensation ran — the
     * alternative is a gap in the trail where an operator cannot tell whether it was attempted.
     */
    @Test
    fun `a step with nothing to undo leaves the tasks alone`() {
        val untouched = task(UUID.randomUUID()).also { tasks.given(it) }

        service.compensate(TaskCompensationCommand.LogTaskCompensation(untouched.id.toString()))

        assertTrue(tasks.stored.containsKey(untouched.id))
    }
}
