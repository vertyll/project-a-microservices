package com.vertyll.veds.iam.infrastructure.web.error

internal data class ErrorDetails(
    val code: String,
    val params: Map<String, Any> = emptyMap(),
)

