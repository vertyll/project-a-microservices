package com.vertyll.veds.task.application.port.outbound

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.task.application.saga.model.Saga
import com.vertyll.veds.task.application.saga.model.SagaStepNames
import com.vertyll.veds.task.application.saga.model.SagaTypes

interface SagaProcessPort {
    fun startSaga(
        sagaType: SagaTypes,
        payload: Map<String, Any?>,
    ): Saga

    fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Map<String, Any?> = emptyMap(),
    )

    fun markSagaCompleted(sagaId: String)

    fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    )

    fun markAwaitingResponse(sagaId: String)

    fun findSagaDomainById(sagaId: String): Saga?
}
