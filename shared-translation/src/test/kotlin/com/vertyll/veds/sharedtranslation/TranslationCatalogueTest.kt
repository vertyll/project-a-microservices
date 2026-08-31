package com.vertyll.veds.sharedtranslation

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TranslationCatalogueTest {
    /**
     * Two places believing they own the same message would make the winner depend on declaration
     * order, so the clash has to fail at start-up rather than resolve silently.
     */
    @Test
    fun `refuses a key declared twice in one catalogue`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                translations("project-service") {
                    key("project.not_found") { pl("Nie znaleziono") }
                    key("project.not_found") { pl("Brak") }
                }
            }

        assertContains(failure.message.orEmpty(), "project.not_found")
    }

    /**
     * A pattern that cannot compile is caught where it is written, not when a user first hits the
     * branch that renders it.
     */
    @Test
    fun `refuses a malformed pattern at declaration time`() {
        assertFailsWith<IllegalArgumentException> {
            translations("project-service") {
                key("project.count") { pl("{count, plural, one{") }
            }
        }
    }

    @Test
    fun `carries the declared keys and their languages`() {
        val catalogue =
            translations("project-service") {
                key("project.not_found", description = "Shown when a project id resolves to nothing") {
                    pl("Nie znaleziono projektu.")
                    en("Project not found.")
                }
            }

        assertEquals("project-service", catalogue.sourceService)
        val definition = catalogue.definitions.single()
        assertEquals("project.not_found", definition.key)
        assertEquals("Project not found.", definition.defaultValues["en"])
        assertEquals("Nie znaleziono projektu.", definition.defaultValues["pl"])
    }
}

class CompositeTranslationSourceTest {
    private val shipped = mapOf("pl" to mapOf("mail.subject" to "Powiadomienie"))

    @Test
    fun `prefers a live snapshot over the shipped default`() {
        val source =
            CompositeTranslationSource(
                snapshots = { mapOf("pl" to TranslationSnapshot("pl", "7", mapOf("mail.subject" to "Nowe powiadomienie"))) },
                fallbackDefaults = shipped,
            )

        assertEquals("Nowe powiadomienie", source.patternFor("mail.subject", "pl"))
    }

    /**
     * Keeps mail going out with sensible text when the catalogue service was unreachable at
     * start-up. It is not a per-key fallback: a key absent from both still renders as the key.
     */
    @Test
    fun `falls back to the shipped default when no snapshot was loaded`() {
        val source = CompositeTranslationSource(snapshots = { emptyMap() }, fallbackDefaults = shipped)

        assertEquals("Powiadomienie", source.patternFor("mail.subject", "pl"))
        assertNull(source.patternFor("mail.absent", "pl"))
    }

    @Test
    fun `matches the language case-insensitively`() {
        val source = CompositeTranslationSource(snapshots = { emptyMap() }, fallbackDefaults = shipped)

        assertEquals("Powiadomienie", source.patternFor("mail.subject", "PL"))
    }
}
