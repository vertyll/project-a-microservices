package com.vertyll.veds.translation.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TranslationKeyTest {
    private fun key() = TranslationKey(key = "project.not_found", sourceService = "project-service")

    @Test
    fun `the owning service may re-declare its own key`() {
        val updated = key().redeclaredBy("project-service", "Raised when a project id is unknown")

        assertEquals("Raised when a project id is unknown", updated.description)
    }

    @Test
    fun `another service may not claim it`() {
        assertFailsWith<IllegalStateException> { key().redeclaredBy("task-service", null) }
    }

    @Test
    fun `re-declaring without a description keeps the existing one`() {
        val described = key().redeclaredBy("project-service", "Original")
        val again = described.redeclaredBy("project-service", null)

        assertEquals("Original", again.description)
    }

    @Test
    fun `rejects a key that is not dotted lower case`() {
        assertFailsWith<IllegalArgumentException> { TranslationKey("ProjectNotFound", "project-service") }
        assertFailsWith<IllegalArgumentException> { TranslationKey("project", "project-service") }
        assertFailsWith<IllegalArgumentException> { TranslationKey("", "project-service") }
    }
}
