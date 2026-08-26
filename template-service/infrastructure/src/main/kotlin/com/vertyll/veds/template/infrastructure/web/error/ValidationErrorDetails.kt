package com.vertyll.veds.template.infrastructure.web.error

internal data class ValidationErrorDetails(
    val code: String,
    val fields: Map<String, String>,
)
