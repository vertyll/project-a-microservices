package com.vertyll.veds.translation.infrastructure.web.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class RegisterCatalogueRequest(
    @field:NotBlank(message = "validation.translation.source_service_required")
    val sourceService: String = "",
    @field:NotEmpty(message = "validation.translation.entries_required")
    @field:Valid
    val entries: List<CatalogueEntryRequest> = emptyList(),
)

data class CatalogueEntryRequest(
    @field:NotBlank(message = "validation.translation.key_required")
    val key: String = "",
    val description: String? = null,
    @field:NotEmpty(message = "validation.translation.default_values_required")
    val defaultValues: Map<String, String> = emptyMap(),
)
