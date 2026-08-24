package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectType(
    val id: UUID = UUID.randomUUID(),
    val code: ProjectTypeCode,
    val isActive: Boolean = true,
    val translations: Set<Translation> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        requireAtLeastOneTranslation(translations)
    }

    fun translationFor(language: LanguageTag): Translation = translations.resolveFor(language)

    fun deactivate(): ProjectType = copy(isActive = false, updatedAt = Instant.now())

    companion object {
        fun create(
            code: ProjectTypeCode,
            translations: Set<Translation>,
        ): ProjectType = ProjectType(code = code, translations = translations)
    }
}
