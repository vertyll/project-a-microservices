package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Strongly-typed envelope produced by [CompensationCommandDeserializer].
 *
 * @param sagaId  originating saga's id (Saga Log Correlation).
 * @param stepId  optional id of the step being compensated; `null` for saga-level actions.
 * @param command service-local typed compensation command (sealed hierarchy).
 */
data class DecodedCompensationEvent<TCommand : Any>(
    val sagaId: String,
    val stepId: Long?,
    val command: TCommand,
)
