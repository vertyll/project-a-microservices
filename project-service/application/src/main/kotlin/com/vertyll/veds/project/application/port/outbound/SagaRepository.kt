package com.vertyll.veds.project.application.port.outbound

import com.vertyll.veds.project.application.saga.model.Saga
import com.vertyll.veds.shared.saga.SagaStatus
import java.time.Instant

interface SagaRepository {
    fun save(saga: Saga): Saga

    fun findById(id: String): Saga?

    fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<Saga>

    fun findByStatusAndUpdatedAtBefore(
        status: SagaStatus,
        updatedAt: Instant,
    ): List<Saga>
}
