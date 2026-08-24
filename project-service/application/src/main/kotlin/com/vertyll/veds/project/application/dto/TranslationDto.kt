package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.model.Translation
import jakarta.validation.constraints.NotBlank

data class TranslationDto(
    val language: LanguageCode,
    @field:NotBlank(message = "Translation name is required")
    val name: String,
    val description: String? = null,
) {
    fun toDomain(): Translation =
        Translation(
            language = language,
            name = name,
            description = description,
        )

    companion object {
        fun from(translation: Translation): TranslationDto =
            TranslationDto(
                language = translation.language,
                name = translation.name,
                description = translation.description,
            )
    }
}
