package com.vertyll.veds.task.infrastructure.saga

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.SagaCompensationStepFactory
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaStepJpaEntity
import java.time.Instant

internal class TaskSagaCompensationStepFactory : SagaCompensationStepFactory<SagaStepJpaEntity> {
    override fun createCompensationStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): SagaStepJpaEntity =
        SagaStepJpaEntity(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            createdAt = createdAt,
            completedAt = completedAt,
            compensationStepId = compensationStepId,
        )
}
