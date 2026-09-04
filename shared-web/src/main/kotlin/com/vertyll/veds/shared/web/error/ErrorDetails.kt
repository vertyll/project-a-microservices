package com.vertyll.veds.shared.web.error

/**
 * A refused request, named by the key of the service's own error catalogue.
 *
 * @property params values the translated message interpolates.
 */
data class ErrorDetails(
    val code: String,
    val params: Map<String, Any> = emptyMap(),
)

/**
 * A request refused because of its contents, with the offending fields.
 *
 * @property fields field name to the translation key of what is wrong with it.
 */
data class ValidationErrorDetails(
    val code: String,
    val fields: Map<String, String>,
)
