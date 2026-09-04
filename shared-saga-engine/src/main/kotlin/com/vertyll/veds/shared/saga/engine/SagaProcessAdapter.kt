package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.Saga
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaSnapshot
import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.SagaTypeValue

/**
 * Serves [SagaProcessPort] from a [SagaEngine].
 *
 * Generic over the storage types so one implementation covers every service:
 * each registers it as a bean in its own saga configuration, typed on the
 * entities that service persists.
 *
 * The engine returns its live aggregate; this adapter copies it into a
 * [SagaSnapshot] before it crosses into the application layer, so no caller
 * holds a handle it could transition behind the engine's back.
 */
class SagaProcessAdapter<S : Saga<S>, T : SagaStep<T>>(
    private val engine: SagaEngine<S, T>,
) : SagaProcessPort {
    override fun startSaga(
        sagaType: SagaTypeValue,
        payload: Map<String, Any?>,
    ): SagaSnapshot = engine.startSaga(sagaType = sagaType, payload = payload).toSnapshot()

    override fun recordSagaStep(
        sagaId: String,
        stepName: SagaTypeValue,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        engine.recordSagaStep(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
        )
    }

    override fun markSagaCompleted(sagaId: String) {
        engine.completeSaga(sagaId)
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        engine.failSaga(sagaId, errorMessage)
    }

    override fun markAwaitingResponse(sagaId: String) {
        engine.awaitResponse(sagaId)
    }

    override fun findSagaById(sagaId: String): SagaSnapshot? = engine.findSagaById(sagaId)?.toSnapshot()

    private fun Saga<*>.toSnapshot() =
        SagaSnapshot(
            id = id,
            type = type,
            status = status,
            payload = payload,
            lastError = lastError,
            startedAt = startedAt,
            completedAt = completedAt,
            updatedAt = updatedAt,
            version = version,
        )
}
