package com.vertyll.veds.sharederror

/**
 * One entry in a service's error catalogue.
 *
 * A service declares its own enum of these — the keys are its own, and only it
 * knows what they mean. Implementing this is what lets one exception type and one
 * exception handler serve every service without knowing any of their catalogues.
 *
 * @property key translation key naming the failure, resolved by translation-service.
 * @property kind which HTTP status the failure becomes.
 */
interface DomainError {
    val key: String
    val kind: ErrorKind
}
