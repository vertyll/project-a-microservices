package com.vertyll.veds.shared.saga

/**
 * What an application service may ask of a saga.
 *
 * Named by application layers, so it lives here rather than in
 * `shared-saga-engine`: the implementation drags in Spring and JPA, this
 * interface drags in nothing.
 *
 * Types and step names arrive as [SagaTypeValue], which each bounded context
 * satisfies with its own enum. Narrowing the parameter to that enum would buy
 * no safety — a service only ever has its own on the classpath — and would cost
 * one copy of this interface per service.
 */
interface SagaProcessPort {
    /** Starts a saga of [sagaType] and returns it as first written. */
    fun startSaga(
        sagaType: SagaTypeValue,
        payload: Map<String, Any?>,
    ): SagaSnapshot

    /**
     * Records that [stepName] reached [status]. Recording the same step twice
     * updates the existing row rather than adding a second one, which is what
     * makes a redelivered event safe to handle.
     */
    fun recordSagaStep(
        sagaId: String,
        stepName: SagaTypeValue,
        status: SagaStepStatus,
        payload: Map<String, Any?> = emptyMap(),
    )

    /** Settles the saga as [SagaStatus.COMPLETED]. */
    fun markSagaCompleted(sagaId: String)

    /** Settles the saga as [SagaStatus.FAILED], recording [errorMessage]. */
    fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    )

    /**
     * Marks the saga as waiting on another service. The watchdog measures the
     * timeout from this moment, so a saga that waits without saying so is one
     * nothing will ever retry.
     */
    fun markAwaitingResponse(sagaId: String)

    /** The saga's current state, or `null` if no saga carries that id. */
    fun findSagaById(sagaId: String): SagaSnapshot?
}
