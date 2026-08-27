package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.persistence.BaseSagaStepRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaStepJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface SagaStepJpaRepository :
    JpaRepository<SagaStepJpaEntity, Long>,
    BaseSagaStepRepository<SagaStepJpaEntity> {
    override fun findBySagaId(sagaId: String): List<SagaStepJpaEntity>

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<SagaStepJpaEntity>

    fun findBySagaIdAndStepNameAndStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ): List<SagaStepJpaEntity>

    fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<SagaStepJpaEntity>
}
