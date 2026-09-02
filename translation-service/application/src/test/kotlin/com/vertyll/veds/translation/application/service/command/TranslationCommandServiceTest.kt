@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.translation.application.service.command

import com.vertyll.veds.translation.application.ENGLISH
import com.vertyll.veds.translation.application.InMemoryKeyRepository
import com.vertyll.veds.translation.application.InMemoryLanguageRepository
import com.vertyll.veds.translation.application.InMemoryValueRepository
import com.vertyll.veds.translation.application.POLISH
import com.vertyll.veds.translation.application.SilentLogger
import com.vertyll.veds.translation.application.command.CatalogueEntryCommand
import com.vertyll.veds.translation.application.command.ClearOverrideCommand
import com.vertyll.veds.translation.application.command.ImportTranslationsCommand
import com.vertyll.veds.translation.application.command.ImportedTranslationCommand
import com.vertyll.veds.translation.application.command.OverrideTranslationCommand
import com.vertyll.veds.translation.application.command.RegisterCatalogueCommand
import com.vertyll.veds.translation.application.exception.ApiException
import com.vertyll.veds.translation.domain.error.TranslationError
import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.model.TranslationValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TranslationCommandServiceTest {
    private val keys = InMemoryKeyRepository()
    private val values = InMemoryValueRepository()
    private val languages = InMemoryLanguageRepository()

    private val service = TranslationCommandService(keys, values, languages, SilentLogger)

    private val editor = Uuid.generateV7().toJavaUuid()

    init {
        languages.given(
            Language(tag = ENGLISH, displayName = "English", isDefault = true),
            Language(tag = POLISH, displayName = "Polski"),
        )
    }

    private fun entry(
        key: String = "project.not_found",
        description: String? = null,
        defaults: Map<String, String> = mapOf("en" to "Project not found"),
    ) = CatalogueEntryCommand(key = key, description = description, defaultValues = defaults)

    private fun register(
        sourceService: String = "project-service",
        vararg entries: CatalogueEntryCommand,
    ) = service.registerCatalogue(RegisterCatalogueCommand(sourceService, entries.toList()))

    // ── Registering a catalogue ─────────────────────────────────────────

    @Test
    fun `a service's keys are registered under its own name`() {
        register(entries = arrayOf(entry()))

        val key = keys.findByKey("project.not_found")!!
        assertEquals("project-service", key.sourceService)
    }

    @Test
    fun `defaults are seeded for every language they are given in`() {
        assertEquals(2, register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found", "pl" to "Nie znaleziono")))))

        assertEquals("Not found", values.find("project.not_found", ENGLISH)!!.defaultValue)
        assertEquals("Nie znaleziono", values.find("project.not_found", POLISH)!!.defaultValue)
    }

    @Test
    fun `re-registering an unchanged catalogue writes nothing`() {
        register(entries = arrayOf(entry()))

        assertEquals(0, register(entries = arrayOf(entry())))
    }

    @Test
    fun `a changed default is written on the next registration`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found"))))

        assertEquals(1, register(entries = arrayOf(entry(defaults = mapOf("en" to "No such project")))))
        assertEquals("No such project", values.find("project.not_found", ENGLISH)!!.defaultValue)
    }

    @Test
    fun `re-registering does not undo an administrator's correction`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found"))))
        service.override(OverrideTranslationCommand("project.not_found", "en", "We could not find that project"), editor, null)

        register(entries = arrayOf(entry(defaults = mapOf("en" to "Project missing"))))

        val stored = values.find("project.not_found", ENGLISH)!!
        assertEquals("We could not find that project", stored.effectiveValue)
        assertEquals("Project missing", stored.defaultValue)
    }

    @Test
    fun `another service cannot take over a key`() {
        register(sourceService = "project-service", entries = arrayOf(entry()))

        val error = assertFailsWith<ApiException> { register(sourceService = "task-service", entries = arrayOf(entry())) }

        assertEquals(TranslationError.KEY_OWNED_BY_ANOTHER_SERVICE, error.error)
        assertEquals("project-service", keys.findByKey("project.not_found")!!.sourceService)
    }

    @Test
    fun `the owning service may update its own description`() {
        register(entries = arrayOf(entry(description = "Old")))

        register(entries = arrayOf(entry(description = "Shown when a project id does not resolve")))

        assertEquals("Shown when a project id does not resolve", keys.findByKey("project.not_found")!!.description)
    }

    @Test
    fun `a default for a language this deployment does not serve is skipped`() {
        assertEquals(1, register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found", "de" to "Nicht gefunden")))))

        assertNull(
            values.find(
                "project.not_found",
                com.vertyll.veds.translation.domain.model
                    .LanguageTag("de"),
            ),
        )
    }

    // ── Overriding ──────────────────────────────────────────────────────

    private fun givenKey(key: String = "project.not_found") =
        TranslationKey(key = key, sourceService = "project-service").also { keys.given(it) }

    @Test
    fun `an override becomes the value users see and records who wrote it`() {
        givenKey()
        values.given(TranslationValue.seeded("project.not_found", ENGLISH, "Not found"))

        service.override(OverrideTranslationCommand("project.not_found", "en", "We could not find that"), editor, null)

        val stored = values.find("project.not_found", ENGLISH)!!
        assertEquals("We could not find that", stored.effectiveValue)
        assertEquals(editor, stored.updatedBy)
        assertTrue(stored.isOverridden)
    }

    @Test
    fun `a key no service has declared cannot be overridden`() {
        val error =
            assertFailsWith<ApiException> { service.override(OverrideTranslationCommand("made.up_key", "en", "Hello"), editor, null) }

        assertEquals(TranslationError.KEY_NOT_FOUND, error.error)
    }

    @Test
    fun `a language this deployment does not serve cannot be overridden`() {
        givenKey()

        val error =
            assertFailsWith<ApiException> { service.override(OverrideTranslationCommand("project.not_found", "de", "Hallo"), editor, null) }

        assertEquals(TranslationError.LANGUAGE_NOT_FOUND, error.error)
    }

    @Test
    fun `a malformed ICU pattern is rejected where it is typed`() {
        givenKey()

        val error =
            assertFailsWith<ApiException> {
                service.override(OverrideTranslationCommand("project.not_found", "en", "Hello {name"), editor, null)
            }

        assertEquals(TranslationError.INVALID_ICU_PATTERN, error.error)
        assertNull(values.find("project.not_found", ENGLISH))
    }

    @Test
    fun `an override against a stale version is refused`() {
        givenKey()
        values.given(TranslationValue(key = "project.not_found", language = ENGLISH, version = 3L))

        val error =
            assertFailsWith<ApiException> {
                service.override(OverrideTranslationCommand("project.not_found", "en", "New"), editor, version = 1L)
            }

        assertEquals(TranslationError.VERSION_MISMATCH, error.error)
    }

    // ── Clearing an override ────────────────────────────────────────────

    @Test
    fun `clearing an override restores the shipped default`() {
        givenKey()
        values.given(
            TranslationValue.seeded("project.not_found", ENGLISH, "Not found").overriddenBy(editor, "Custom"),
        )

        service.clearOverride(ClearOverrideCommand("project.not_found", "en"), editor)

        val stored = values.find("project.not_found", ENGLISH)!!
        assertEquals("Not found", stored.effectiveValue)
        assertTrue(!stored.isOverridden)
    }

    @Test
    fun `clearing a value that was never written is refused`() {
        givenKey()

        val error = assertFailsWith<ApiException> { service.clearOverride(ClearOverrideCommand("project.not_found", "en"), editor) }

        assertEquals(TranslationError.VALUE_NOT_FOUND, error.error)
    }

    // ── Importing ───────────────────────────────────────────────────────

    private fun import(vararg rows: Triple<String, String, String>) =
        service.import(
            ImportTranslationsCommand(
                entries = rows.map { ImportedTranslationCommand(it.first, it.second, it.third) },
                importedBy = editor,
            ),
        )

    @Test
    fun `imported rows become overrides`() {
        givenKey()

        val report = import(Triple("project.not_found", "en", "Imported wording"))

        assertEquals(1, report.applied)
        assertEquals("Imported wording", values.find("project.not_found", ENGLISH)!!.effectiveValue)
    }

    @Test
    fun `a bad row does not abort the import and is reported`() {
        givenKey()

        val report =
            import(
                Triple("project.not_found", "en", "Imported wording"),
                Triple("made.up_key", "en", "Orphan"),
                Triple("project.not_found", "de", "Nicht gefunden"),
            )

        assertEquals(1, report.applied)
        assertEquals(listOf("made.up_key"), report.skippedUnknownKeys)
        assertEquals(listOf("de"), report.skippedUnknownLanguages)
    }

    @Test
    fun `a row with a malformed pattern is rejected with its reason`() {
        givenKey()

        val report = import(Triple("project.not_found", "en", "Hello {name"))

        assertEquals(0, report.applied)
        assertEquals(1, report.rejectedPatterns.size)
        assertEquals("project.not_found", report.rejectedPatterns.single().key)
        assertNull(values.find("project.not_found", ENGLISH))
    }

    @Test
    fun `an empty import reports nothing applied`() {
        val report = import()

        assertEquals(0, report.applied)
        assertTrue(report.skippedUnknownKeys.isEmpty())
        assertTrue(report.rejectedPatterns.isEmpty())
    }
}
