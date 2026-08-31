package com.vertyll.veds.sharedtranslation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageResolverTest {
    private fun resolver(vararg entries: Pair<String, String>) = MessageResolver(TranslationSnapshot("pl", "1", entries.toMap()))

    @Test
    fun `substitutes named arguments into the pattern`() {
        val resolved =
            resolver("task.assigned" to "Zadanie {name} przypisane").resolve(
                "task.assigned",
                "pl",
                mapOf("name" to "Apollo"),
            )

        assertEquals("Zadanie Apollo przypisane", resolved)
    }

    /**
     * The reason this module carries ICU4J at all. Polish has four plural categories, and
     * `java.text.MessageFormat` has none of them: it would render "5 zadania".
     */
    @Test
    fun `applies Polish plural categories`() {
        val pattern = "{count, plural, one{# zadanie} few{# zadania} many{# zadań} other{# zadania}}"
        val resolver = resolver("task.count" to pattern)

        assertEquals("1 zadanie", resolver.resolve("task.count", "pl", mapOf("count" to 1)))
        assertEquals("3 zadania", resolver.resolve("task.count", "pl", mapOf("count" to 3)))
        assertEquals("5 zadań", resolver.resolve("task.count", "pl", mapOf("count" to 5)))
    }

    /**
     * Deliberately not a fallback. Another language would look like a finished translation;
     * the bare key is greppable and names exactly what is missing. Throwing would take a page
     * down over one absent string.
     */
    @Test
    fun `renders an unknown key as the key itself`() {
        assertEquals("task.missing", resolver().resolve("task.missing", "pl"))
    }

    @Test
    fun `renders the key when the language does not match the snapshot`() {
        assertEquals("task.hello", resolver("task.hello" to "Cześć").resolve("task.hello", "en"))
    }

    /**
     * A stored pattern that will not format is a data defect. Surfacing the key points at the
     * translation to fix, where an exception would only say that something, somewhere, broke.
     */
    @Test
    fun `renders the key when the stored pattern is malformed`() {
        assertEquals("task.broken", resolver("task.broken" to "{count, plural, one{").resolve("task.broken", "pl"))
    }

    @Test
    fun `reports whether a translation exists`() {
        val resolver = resolver("task.hello" to "Cześć")

        assertTrue(resolver.hasTranslation("task.hello", "pl"))
        assertFalse(resolver.hasTranslation("task.hello", "en"))
        assertFalse(resolver.hasTranslation("task.absent", "pl"))
    }
}
