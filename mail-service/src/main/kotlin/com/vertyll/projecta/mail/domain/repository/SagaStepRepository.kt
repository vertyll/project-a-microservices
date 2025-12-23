package com.vertyll.projecta.mail.domain.repository

import com.vertyll.projecta.mail.domain.model.entity.SagaStep
import com.vertyll.projecta.mail.domain.model.enums.SagaStepStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : JpaRepository<SagaStep, Long> {
    fun findBySagaId(sagaId: String): List<SagaStep>

    fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<SagaStep>

    fun findBySagaIdAndStatus(
        sagaId: String,
        status: SagaStepStatus,
    ): List<SagaStep>

    fun findBySagaIdAndStepNameAndStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ): List<SagaStep>

    fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<SagaStep>

    fun findByCompensationStepId(compensationStepId: Long): SagaStep?
}
