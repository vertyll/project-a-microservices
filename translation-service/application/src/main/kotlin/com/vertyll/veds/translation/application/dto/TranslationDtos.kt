package com.vertyll.veds.translation.application.dto

import java.time.Instant

data class TranslationSnapshotResponse(
    val language: String,
    val version: String,
    val entries: Map<String, String>,
)

data class TranslationKeyDetailsResponse(
    val key: String,
    val sourceService: String,
    val description: String?,
    val values: List<TranslationValueResponse>,
    val missingLanguages: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TranslationValueResponse(
    val language: String,
    val defaultValue: String?,
    val overrideValue: String?,
    val effectiveValue: String?,
    val isOverridden: Boolean,
    val updatedAt: Instant,
    val version: Long?,
)

data class LanguageResponse(
    val tag: String,
    val displayName: String,
    val isDefault: Boolean,
)

data class PagedResponse<T>(
    val items: List<T>,
    val pagination: PaginationMeta,
)

data class PaginationMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)

data class ImportReportResponse(
    val applied: Int,
    val skippedUnknownKeys: List<String>,
    val skippedUnknownLanguages: List<String>,
    val rejectedPatterns: List<RejectedPatternResponse>,
)

data class RejectedPatternResponse(
    val key: String,
    val language: String,
    val reason: String,
)
