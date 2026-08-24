package com.vertyll.veds.translation.domain.model

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TranslationValueTest {
    private val editor = UUID.randomUUID()
    private val pl = LanguageTag.of("pl")

    private fun seeded() = TranslationValue.seeded("project.not_found", pl, "Nie znaleziono projektu.")

    @Test
    fun `a seeded value is effective until somebody overrides it`() {
        val value = seeded()

        assertEquals("Nie znaleziono projektu.", value.effectiveValue)
        assertFalse(value.isOverridden)
    }

    @Test
    fun `an override wins over the shipped default`() {
        val overridden = seeded().overriddenBy(editor, "Taki projekt nie istnieje.")

        assertEquals("Taki projekt nie istnieje.", overridden.effectiveValue)
        assertTrue(overridden.isOverridden)
        assertEquals(editor, overridden.updatedBy)
    }

    @Test
    fun `re-seeding leaves the override alone`() {
        val overridden = seeded().overriddenBy(editor, "Taki projekt nie istnieje.")
        val reseeded = overridden.withSeededDefault("Project not found.")

        assertEquals("Taki projekt nie istnieje.", reseeded.effectiveValue)
        assertEquals("Project not found.", reseeded.defaultValue)
    }

    @Test
    fun `re-seeding the same value returns the same instance`() {
        val value = seeded()

        assertSame(value, value.withSeededDefault("Nie znaleziono projektu."))
    }

    @Test
    fun `clearing an override falls back to the shipped default`() {
        val reverted = seeded().overriddenBy(editor, "Coś innego.").overrideCleared(editor)

        assertEquals("Nie znaleziono projektu.", reverted.effectiveValue)
        assertFalse(reverted.isOverridden)
    }

    @Test
    fun `a value with neither column has no effective text`() {
        val empty = TranslationValue(key = "project.not_found", language = pl)

        assertNull(empty.effectiveValue)
    }

    @Test
    fun `an override may not be blank`() {
        assertFailsWith<IllegalArgumentException> { seeded().overriddenBy(editor, "   ") }
    }
}
