package com.vertyll.veds.translation.application.command

import java.util.UUID

data class RegisterCatalogueCommand(
    val sourceService: String,
    val entries: List<CatalogueEntryCommand>,
)

data class CatalogueEntryCommand(
    val key: String,
    val description: String?,
    val defaultValues: Map<String, String>,
)

data class OverrideTranslationCommand(
    val key: String,
    val language: String,
    val value: String,
)

data class ClearOverrideCommand(
    val key: String,
    val language: String,
)

data class ImportTranslationsCommand(
    val entries: List<ImportedTranslationCommand>,
    val importedBy: UUID,
)

data class ImportedTranslationCommand(
    val key: String,
    val language: String,
    val value: String,
)
