package com.vertyll.veds.mail.infrastructure.web.error

internal data class ValidationErrorDetails(
    val code: String,
    val fields: Map<String, String>,
)