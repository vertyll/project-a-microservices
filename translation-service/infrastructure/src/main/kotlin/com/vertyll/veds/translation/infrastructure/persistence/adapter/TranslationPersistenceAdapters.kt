package com.vertyll.veds.translation.infrastructure.persistence.adapter

import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.PageResult
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.model.TranslationValue
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository
import com.vertyll.veds.translation.infrastructure.persistence.entity.LanguageJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationKeyJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationValueJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.repository.LanguageJpaRepository
import com.vertyll.veds.translation.infrastructure.persistence.repository.TranslationKeyJpaRepository
import com.vertyll.veds.translation.infrastructure.persistence.repository.TranslationValueJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Component
internal class TranslationKeyPersistenceAdapter(
    private val repository: TranslationKeyJpaRepository,
) : TranslationKeyRepository {
    override fun save(key: TranslationKey): TranslationKey = repository.save(key.toEntity()).toDomain()

    override fun saveAll(keys: Collection<TranslationKey>): List<TranslationKey> =
        repository.saveAll(keys.map { it.toEntity() }).map { it.toDomain() }

    override fun findByKey(key: String): TranslationKey? = repository.findByIdOrNull(key)?.toDomain()

    override fun search(
        searchTerm: String?,
        sourceService: String?,
        pageRequest: PageRequest,
    ): PageResult<TranslationKey> {
        val page =
            repository.search(
                searchTerm = searchTerm,
                sourceService = sourceService,
                pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size),
            )
        return PageResult(
            content = page.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = page.totalElements,
        )
    }

    override fun findAll(): List<TranslationKey> = repository.findAll().map { it.toDomain() }
}

@Component
internal class TranslationValuePersistenceAdapter(
    private val repository: TranslationValueJpaRepository,
) : TranslationValueRepository {
    override fun save(value: TranslationValue): TranslationValue = repository.save(value.toEntity()).toDomain()

    override fun saveAll(values: Collection<TranslationValue>): List<TranslationValue> =
        repository.saveAll(values.map { it.toEntity() }).map { it.toDomain() }

    override fun find(
        key: String,
        language: LanguageTag,
    ): TranslationValue? = repository.findByKeyAndLanguage(key, language.value).orElse(null)?.toDomain()

    override fun findAllForKeys(keys: Collection<String>): List<TranslationValue> =
        if (keys.isEmpty()) emptyList() else repository.findAllByKeyIn(keys).map { it.toDomain() }

    override fun findAllForLanguage(language: LanguageTag): List<TranslationValue> =
        repository.findAllByLanguage(language.value).map { it.toDomain() }

    override fun latestChangeMarker(language: LanguageTag): String =
        repository.latestUpdate(language.value)?.toEpochMilli()?.toString() ?: "0"
}

@Component
internal class LanguagePersistenceAdapter(
    private val repository: LanguageJpaRepository,
) : LanguageRepository {
    override fun save(language: Language): Language = repository.save(language.toEntity()).toDomain()

    override fun findByTag(tag: LanguageTag): Language? = repository.findByIdOrNull(tag.value)?.toDomain()

    override fun findAll(): List<Language> = repository.findAll().map { it.toDomain() }

    override fun findDefault(): Language? = repository.findByIsDefaultTrue().orElse(null)?.toDomain()
}

private fun TranslationKey.toEntity() =
    TranslationKeyJpaEntity(
        key = key,
        sourceService = sourceService,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun TranslationKeyJpaEntity.toDomain() =
    TranslationKey(
        key = key,
        sourceService = sourceService,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun TranslationValue.toEntity() =
    TranslationValueJpaEntity(
        id = id,
        key = key,
        language = language.value,
        defaultValue = defaultValue,
        overrideValue = overrideValue,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

private fun TranslationValueJpaEntity.toDomain() =
    TranslationValue(
        id = id,
        key = key,
        language = LanguageTag.of(language),
        defaultValue = defaultValue,
        overrideValue = overrideValue,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

private fun Language.toEntity() =
    LanguageJpaEntity(
        tag = tag.value,
        displayName = displayName,
        isDefault = isDefault,
        createdAt = createdAt,
    )

private fun LanguageJpaEntity.toDomain() =
    Language(
        tag = LanguageTag.of(tag),
        displayName = displayName,
        isDefault = isDefault,
        createdAt = createdAt,
    )
