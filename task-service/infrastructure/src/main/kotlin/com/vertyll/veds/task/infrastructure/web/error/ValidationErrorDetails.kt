package com.vertyll.veds.task.infrastructure.web.error

internal data class ValidationErrorDetails(
    val code: String,
    val fields: Map<String, String>,
)