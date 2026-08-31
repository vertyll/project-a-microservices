package com.vertyll.veds.template.application.service

import com.vertyll.veds.template.application.InMemoryTemplateRepository
import com.vertyll.veds.template.application.SilentLogger
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.domain.model.Template
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The compensation half of the reference service. It is copied as-is into new services, so it has
 * to demonstrate the two properties every compensation needs: safe to repeat, and safe to run
 * against state that is already gone.
 */
internal class TemplateCompensationServiceTest {
    private val templates = InMemoryTemplateRepository()
    private val service = TemplateCompensationService(templates, SilentLogger)

    private fun givenTemplate() = Template(name = "welcome", payload = "{}").also { templates.given(it) }

    @Test
    fun `a template from a failed workflow is deleted`() {
        val created = givenTemplate()

        service.compensate(TemplateCompensationCommand.DeleteTemplate(created.id))

        assertNull(templates.findById(created.id))
    }

    /** Delivery is at-least-once, so the second copy has to find the work already done. */
    @Test
    fun `deleting the same template twice is harmless`() {
        val created = givenTemplate()
        val command = TemplateCompensationCommand.DeleteTemplate(created.id)

        service.compensate(command)
        service.compensate(command)

        assertTrue(templates.stored.isEmpty())
    }

    /** The state this undo exists to reverse is already gone — the outcome that was wanted. */
    @Test
    fun `a template that no longer exists is not an error`() {
        service.compensate(TemplateCompensationCommand.DeleteTemplate(UUID.randomUUID().toString()))

        assertTrue(templates.stored.isEmpty())
    }

    /**
     * An event that has already been consumed cannot be recalled. The step is recorded as
     * compensated anyway, so the saga can finish instead of retrying something no code can undo.
     */
    @Test
    fun `a published event has no rollback and leaves the template alone`() {
        val created = givenTemplate()

        service.compensate(TemplateCompensationCommand.LogTemplateCompensation(created.id))

        assertTrue(templates.stored.containsKey(created.id))
    }
}
