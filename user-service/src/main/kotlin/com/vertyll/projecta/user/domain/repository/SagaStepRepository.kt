package com.vertyll.projecta.user.domain.repository

import com.vertyll.projecta.user.domain.model.SagaStep
import com.vertyll.projecta.user.domain.model.SagaStepStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaStepRepository : JpaRepository<SagaStep, Long> {
    fun findBySagaId(sagaId: String): List<SagaStep>

    fun findBySagaIdAndStepName(sagaId: String, stepName: String): List<SagaStep>

    fun findBySagaIdAndStatus(sagaId: String, status: SagaStepStatus): List<SagaStep>

    fun findByStepNameAndStatus(stepName: String, status: SagaStepStatus): List<SagaStep>

    fun findByCompensationStepId(compensationStepId: Long): SagaStep?
} 