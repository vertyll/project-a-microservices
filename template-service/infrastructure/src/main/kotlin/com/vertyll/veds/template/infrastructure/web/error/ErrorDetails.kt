package com.vertyll.veds.template.infrastructure.web.error

internal data class ErrorDetails(
    val code: String,
    val params: Map<String, Any> = emptyMap(),
)
