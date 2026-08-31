package com.vertyll.veds.template.application.service.command

import com.vertyll.veds.template.application.InMemoryTemplateRepository
import com.vertyll.veds.template.application.RecordingSagaProcess
import com.vertyll.veds.template.application.SilentLogger
import com.vertyll.veds.template.application.command.CreateTemplateCommand
import com.vertyll.veds.template.domain.model.TemplateStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * template-service is the shape every new service is cloned from, so what it demonstrates has to be
 * right: a saga that records each step, closes itself on both paths, and never leaves a workflow
 * open for the watchdog to time out. A bug here is copied into the next service.
 */
internal class TemplateCommandServiceTest {
    private val templates = InMemoryTemplateRepository()
    private val saga = RecordingSagaProcess()

    private val service = TemplateCommandService(saga, templates, SilentLogger)

    private fun create(name: String = "welcome") = service.processTemplateWithSaga(CreateTemplateCommand(name, "{}"))

    @Test
    fun `a processed template is stored in its final state`() {
        val result = create()

        assertEquals(TemplateStatus.PROCESSED, result.status)
        assertEquals(TemplateStatus.PROCESSED, templates.findById(result.id)!!.status)
    }

    @Test
    fun `the saga records every step and closes itself`() {
        create()

        assertEquals(
            listOf(
                "start(TemplateProcessing)",
                "step(ProcessTemplate,COMPLETED)",
                "step(PersistTemplate,COMPLETED)",
                "completed",
            ),
            saga.trail,
        )
    }

    /**
     * A saga left open after a failure is one the watchdog has to time out much later. Closing it
     * here is what turns a crash into a workflow that can be compensated straight away.
     */
    @Test
    fun `a failure closes the saga and records why`() {
        templates.saveFails = IllegalStateException("duplicate template name")

        assertFailsWith<IllegalStateException> { create() }

        assertEquals("step(PersistTemplate,FAILED)", saga.trail[saga.trail.size - 2])
        assertEquals("failed(duplicate template name)", saga.trail.last())
    }

    /** The caller has to see the failure; swallowing it would report success for work that failed. */
    @Test
    fun `a failure is re-thrown to the caller`() {
        templates.saveFails = IllegalStateException("duplicate template name")

        val error = assertFailsWith<IllegalStateException> { create() }

        assertEquals("duplicate template name", error.message)
        assertTrue(templates.stored.isEmpty())
    }

    @Test
    fun `each request gets its own template`() {
        assertTrue(create("first").id != create("second").id)
        assertEquals(2, templates.stored.size)
    }
}
