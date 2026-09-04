package com.vertyll.veds.sharederror

/**
 * Refuses a request, naming the error from the service's own catalogue.
 *
 * Carries no HTTP status: the status follows from [ErrorKind], and the layer that
 * throws this has no business knowing about HTTP.
 *
 * @property params values the translated message interpolates, for example the
 *           identifier that was not found.
 */
class ApiException(
    val error: DomainError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
