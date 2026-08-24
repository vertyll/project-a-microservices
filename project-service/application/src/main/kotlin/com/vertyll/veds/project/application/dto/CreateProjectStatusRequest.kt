package com.vertyll.veds.project.application.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class CreateProjectStatusRequest(
    @field:NotBlank(message = "Color is required")
    val color: String = "",
    @field:NotEmpty(message = "At least one translation is required")
    @field:Valid
    val translations: List<TranslationDto> = emptyList(),
)
