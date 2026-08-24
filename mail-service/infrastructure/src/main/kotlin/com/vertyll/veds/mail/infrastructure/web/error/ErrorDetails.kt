package com.vertyll.veds.mail.infrastructure.web.error

internal data class ErrorDetails(
    val code: String,
    val params: Map<String, Any> = emptyMap(),
)

internal data class ValidationErrorDetails(
    val code: String,
    val fields: Map<String, String>,
)
