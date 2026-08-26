package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Service-provided deserializer for compensation events consumed from the
 * internal `saga-compensation-<participant>` topic.
 *
 * Counterpart of [CompensationEventSerializer]. Implementations live in
 * each service's infrastructure layer (the Anti-Corruption Layer between
 * the technical Avro wire format and the application-layer
 * [TCommand] sealed type) so the shared
 * [SagaCompensationEngine] never sees raw maps or stringly-typed actions.
 */
@Suppress("kotlin:S6517")
fun interface CompensationCommandDeserializer<TCommand : Any> {
    /**
     * Decodes a raw compensation-event [payload] into a strongly-typed
     * [DecodedCompensationEvent].
     */
    fun deserialize(payload: ByteArray): DecodedCompensationEvent<TCommand>
}
