package com.vertyll.veds.translation.application

import com.vertyll.veds.translation.application.port.outbound.UseCaseLogger
import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.PageResult
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.model.TranslationValue
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository

internal val ENGLISH = LanguageTag("en")
internal val POLISH = LanguageTag("pl")

internal const val PROJECT_NOT_FOUND_KEY = "project.not_found"
internal const val PROJECT_SERVICE = "project-service"

internal class InMemoryLanguageRepository : LanguageRepository {
    val stored = linkedMapOf<LanguageTag, Language>()

    fun given(vararg languages: Language) = languages.forEach { stored[it.tag] = it }

    override fun save(language: Language) = language.also { stored[it.tag] = it }

    override fun findByTag(tag: LanguageTag) = stored[tag]

    override fun findAll() = stored.values.toList()

    override fun findDefault() = stored.values.firstOrNull { it.isDefault }
}

internal class InMemoryKeyRepository : TranslationKeyRepository {
    val stored = linkedMapOf<String, TranslationKey>()

    fun given(vararg keys: TranslationKey) = keys.forEach { stored[it.key] = it }

    override fun save(key: TranslationKey) = key.also { stored[it.key] = it }

    override fun saveAll(keys: Collection<TranslationKey>) = keys.map { save(it) }

    override fun findByKey(key: String) = stored[key]

    override fun search(
        searchTerm: String?,
        sourceService: String?,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun findAll() = stored.values.toList()
}

internal class InMemoryValueRepository : TranslationValueRepository {
    val stored = linkedMapOf<Pair<String, LanguageTag>, TranslationValue>()

    fun given(vararg values: TranslationValue) = values.forEach { stored[it.key to it.language] = it }

    override fun save(value: TranslationValue) = value.also { stored[it.key to it.language] = it }

    override fun saveAll(values: Collection<TranslationValue>) = values.map { save(it) }

    override fun find(
        key: String,
        language: LanguageTag,
    ) = stored[key to language]

    override fun findAllForKeys(keys: Collection<String>) = stored.values.filter { it.key in keys }

    override fun findAllForLanguage(language: LanguageTag) = stored.values.filter { it.language == language }

    override fun latestChangeMarker(language: LanguageTag) =
        findAllForLanguage(language).maxOfOrNull { it.updatedAt.toEpochMilli() }?.toString().orEmpty()
}

internal object SilentLogger : UseCaseLogger {
    override fun debug(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun info(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun warn(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun error(
        message: String,
        vararg args: Any?,
    ) = Unit
}
