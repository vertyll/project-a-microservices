package com.vertyll.veds.translation.domain.repository

import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.PageResult
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.model.TranslationValue

interface TranslationKeyRepository {
    fun save(key: TranslationKey): TranslationKey

    fun saveAll(keys: Collection<TranslationKey>): List<TranslationKey>

    fun findByKey(key: String): TranslationKey?

    fun search(
        searchTerm: String?,
        sourceService: String?,
        pageRequest: PageRequest,
    ): PageResult<TranslationKey>

    fun findAll(): List<TranslationKey>
}

interface TranslationValueRepository {
    fun save(value: TranslationValue): TranslationValue

    fun saveAll(values: Collection<TranslationValue>): List<TranslationValue>

    fun find(
        key: String,
        language: LanguageTag,
    ): TranslationValue?

    fun findAllForKeys(keys: Collection<String>): List<TranslationValue>

    fun findAllForLanguage(language: LanguageTag): List<TranslationValue>

    fun latestChangeMarker(language: LanguageTag): String
}

interface LanguageRepository {
    fun save(language: Language): Language

    fun findByTag(tag: LanguageTag): Language?

    fun findAll(): List<Language>

    fun findDefault(): Language?
}
