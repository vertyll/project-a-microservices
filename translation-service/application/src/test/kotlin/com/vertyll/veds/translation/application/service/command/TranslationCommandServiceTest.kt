@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.translation.application.service.command

import com.vertyll.veds.translation.application.ENGLISH
import com.vertyll.veds.translation.application.InMemoryKeyRepository
import com.vertyll.veds.translation.application.InMemoryLanguageRepository
import com.vertyll.veds.translation.application.InMemoryValueRepository
import com.vertyll.veds.translation.application.POLISH
import com.vertyll.veds.translation.application.PROJECT_NOT_FOUND_KEY
import com.vertyll.veds.translation.application.PROJECT_SERVICE
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
        key: String = PROJECT_NOT_FOUND_KEY,
        description: String? = null,
        defaults: Map<String, String> = mapOf("en" to "Project not found"),
    ) = CatalogueEntryCommand(key = key, description = description, defaultValues = defaults)

    private fun register(
        sourceService: String = PROJECT_SERVICE,
        vararg entries: CatalogueEntryCommand,
    ) = service.registerCatalogue(RegisterCatalogueCommand(sourceService, entries.toList()))

    // ── Registering a catalogue ─────────────────────────────────────────

    @Test
    fun `a service's keys are registered under its own name`() {
        register(entries = arrayOf(entry()))

        val key = keys.findByKey(PROJECT_NOT_FOUND_KEY)!!
        assertEquals(PROJECT_SERVICE, key.sourceService)
    }

    @Test
    fun `defaults are seeded for every language they are given in`() {
        assertEquals(2, register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found", "pl" to "Nie znaleziono")))))

        assertEquals("Not found", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.defaultValue)
        assertEquals("Nie znaleziono", values.find(PROJECT_NOT_FOUND_KEY, POLISH)!!.defaultValue)
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
        assertEquals("No such project", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.defaultValue)
    }

    @Test
    fun `re-registering does not undo an administrator's correction`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found"))))
        service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "We could not find that project"), editor, null)

        register(entries = arrayOf(entry(defaults = mapOf("en" to "Project missing"))))

        val stored = values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!
        assertEquals("We could not find that project", stored.effectiveValue)
        assertEquals("Project missing", stored.defaultValue)
    }

    @Test
    fun `another service cannot take over a key`() {
        register(sourceService = PROJECT_SERVICE, entries = arrayOf(entry()))

        val error = assertFailsWith<ApiException> { register(sourceService = "task-service", entries = arrayOf(entry())) }

        assertEquals(TranslationError.KEY_OWNED_BY_ANOTHER_SERVICE, error.error)
        assertEquals(PROJECT_SERVICE, keys.findByKey(PROJECT_NOT_FOUND_KEY)!!.sourceService)
    }

    @Test
    fun `the owning service may update its own description`() {
        register(entries = arrayOf(entry(description = "Old")))

        register(entries = arrayOf(entry(description = "Shown when a project id does not resolve")))

        assertEquals("Shown when a project id does not resolve", keys.findByKey(PROJECT_NOT_FOUND_KEY)!!.description)
    }

    @Test
    fun `a default for a language this deployment does not serve is skipped`() {
        assertEquals(1, register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found", "de" to "Nicht gefunden")))))

        assertNull(
            values.find(
                PROJECT_NOT_FOUND_KEY,
                com.vertyll.veds.translation.domain.model
                    .LanguageTag("de"),
            ),
        )
    }

    // ── Overriding ──────────────────────────────────────────────────────

    private fun givenKey(key: String = PROJECT_NOT_FOUND_KEY) =
        TranslationKey(key = key, sourceService = PROJECT_SERVICE).also { keys.given(it) }

    @Test
    fun `an override becomes the value users see and records who wrote it`() {
        givenKey()
        values.given(TranslationValue.seeded(PROJECT_NOT_FOUND_KEY, ENGLISH, "Not found"))

        service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "We could not find that"), editor, null)

        val stored = values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!
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
            assertFailsWith<ApiException> {
                service.override(
                    OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "de", "Hallo"),
                    editor,
                    null,
                )
            }

        assertEquals(TranslationError.LANGUAGE_NOT_FOUND, error.error)
    }

    @Test
    fun `a malformed ICU pattern is rejected where it is typed`() {
        givenKey()

        val error =
            assertFailsWith<ApiException> {
                service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "Hello {name"), editor, null)
            }

        assertEquals(TranslationError.INVALID_ICU_PATTERN, error.error)
        assertNull(values.find(PROJECT_NOT_FOUND_KEY, ENGLISH))
    }

    @Test
    fun `an override against a stale version is refused`() {
        givenKey()
        values.given(TranslationValue(key = PROJECT_NOT_FOUND_KEY, language = ENGLISH, version = 3L))

        val error =
            assertFailsWith<ApiException> {
                service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "New"), editor, version = 1L)
            }

        assertEquals(TranslationError.VERSION_MISMATCH, error.error)
    }

    // ── Clearing an override ────────────────────────────────────────────

    @Test
    fun `clearing an override restores the shipped default`() {
        givenKey()
        values.given(
            TranslationValue.seeded(PROJECT_NOT_FOUND_KEY, ENGLISH, "Not found").overriddenBy(editor, "Custom"),
        )

        service.clearOverride(ClearOverrideCommand(PROJECT_NOT_FOUND_KEY, "en"), editor)

        val stored = values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!
        assertEquals("Not found", stored.effectiveValue)
        assertTrue(!stored.isOverridden)
    }

    @Test
    fun `clearing a value that was never written is refused`() {
        givenKey()

        val error = assertFailsWith<ApiException> { service.clearOverride(ClearOverrideCommand(PROJECT_NOT_FOUND_KEY, "en"), editor) }

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

        val report = import(Triple(PROJECT_NOT_FOUND_KEY, "en", "Imported wording"))

        assertEquals(1, report.applied)
        assertEquals("Imported wording", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.effectiveValue)
    }

    @Test
    fun `a bad row does not abort the import and is reported`() {
        givenKey()

        val report =
            import(
                Triple(PROJECT_NOT_FOUND_KEY, "en", "Imported wording"),
                Triple("made.up_key", "en", "Orphan"),
                Triple(PROJECT_NOT_FOUND_KEY, "de", "Nicht gefunden"),
            )

        assertEquals(1, report.applied)
        assertEquals(listOf("made.up_key"), report.skippedUnknownKeys)
        assertEquals(listOf("de"), report.skippedUnknownLanguages)
    }

    @Test
    fun `a row with a malformed pattern is rejected with its reason`() {
        givenKey()

        val report = import(Triple(PROJECT_NOT_FOUND_KEY, "en", "Hello {name"))

        assertEquals(0, report.applied)
        assertEquals(1, report.rejectedPatterns.size)
        assertEquals(PROJECT_NOT_FOUND_KEY, report.rejectedPatterns.single().key)
        assertNull(values.find(PROJECT_NOT_FOUND_KEY, ENGLISH))
    }

    @Test
    fun `an import reports the gaps it did not close`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found"))))

        val report = import(Triple(PROJECT_NOT_FOUND_KEY, "en", "No such project"))

        assertEquals(1, report.applied)
        assertEquals(listOf("pl"), report.missingAfterImport.map { it.language })
        assertEquals(PROJECT_NOT_FOUND_KEY, report.missingAfterImport.single().key)
    }

    @Test
    fun `an import that fills every language reports no gaps`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "Not found", "pl" to "Nie znaleziono"))))

        val report = import(Triple(PROJECT_NOT_FOUND_KEY, "en", "No such project"))

        assertTrue(report.missingAfterImport.isEmpty())
    }

    // ── Argument drift ──────────────────────────────────────────────────

    @Test
    fun `an override keeping the default's arguments is accepted`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "{count} tasks left"))))

        service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "Tasks remaining: {count}"), editor, null)

        assertEquals("Tasks remaining: {count}", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.effectiveValue)
    }

    @Test
    fun `an override that renames an argument is refused`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "{count} tasks left"))))

        val error =
            assertFailsWith<ApiException> {
                service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "{liczba} tasks left"), editor, null)
            }

        assertEquals(TranslationError.UNKNOWN_ARGUMENTS, error.error)
        assertEquals("{count} tasks left", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.effectiveValue)
    }

    @Test
    fun `an override that drops an argument is refused`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "{name} has {count} tasks"))))

        assertFailsWith<ApiException> {
            service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "{name} is busy"), editor, null)
        }
    }

    @Test
    fun `a key with no shipped default accepts any arguments`() {
        register(entries = arrayOf(entry(defaults = mapOf("pl" to "Nie znaleziono"))))

        service.override(OverrideTranslationCommand(PROJECT_NOT_FOUND_KEY, "en", "{whatever} works"), editor, null)

        assertEquals("{whatever} works", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.effectiveValue)
    }

    @Test
    fun `an imported row with drifting arguments is reported, not applied`() {
        register(entries = arrayOf(entry(defaults = mapOf("en" to "{count} tasks left"))))

        val report = import(Triple(PROJECT_NOT_FOUND_KEY, "en", "{liczba} tasks left"))

        assertEquals(0, report.applied)
        assertEquals(1, report.rejectedPatterns.size)
        assertEquals("{count} tasks left", values.find(PROJECT_NOT_FOUND_KEY, ENGLISH)!!.effectiveValue)
    }

    @Test
    fun `an empty import reports nothing applied`() {
        val report = import()

        assertEquals(0, report.applied)
        assertTrue(report.skippedUnknownKeys.isEmpty())
        assertTrue(report.rejectedPatterns.isEmpty())
    }
}
