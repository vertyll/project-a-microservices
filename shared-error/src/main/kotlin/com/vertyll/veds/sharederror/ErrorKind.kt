package com.vertyll.veds.sharederror

/**
 * What kind of failure an error describes, in terms every context shares.
 *
 * This is transport vocabulary, not domain knowledge: it exists so one place can
 * decide which HTTP status a failure becomes, instead of each service repeating
 * the same mapping. What a failure *means* stays in the service's own error
 * catalogue, which names the key.
 */
enum class ErrorKind {
    NOT_FOUND,
    UNAUTHENTICATED,
    ACCESS_DENIED,
    CONFLICT,
    INVALID,
    PRECONDITION_FAILED,
    GONE,
    MISCONFIGURED,
}
