package com.vertyll.veds.translation.application.dto

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