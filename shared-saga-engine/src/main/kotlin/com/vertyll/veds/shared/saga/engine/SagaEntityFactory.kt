package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.Saga
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.SagaStepStatus
import java.time.Instant

/**
 * Hook used by [SagaEngine] to instantiate concrete saga/saga-step instances
 * for the owning microservice.
 *
 * Each service provides its own implementation against the persistence-agnostic
 * [Saga] / [SagaStep] contracts. The factory is the only place that knows the
 * concrete persistence type (JPA entity, Mongo document, …); the engine itself
 * stays storage-agnostic.
 */
interface SagaEntityFactory<S : Saga<S>, T : SagaStep<T>> {
    /** Builds a fresh saga aggregate ready to be persisted. */
    fun createSaga(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): S

    /** Builds a fresh saga step ready to be persisted. */
    fun createSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): T
}
