package com.vertyll.projecta.mail.domain.repository

import com.vertyll.projecta.mail.domain.model.Saga
import com.vertyll.projecta.mail.domain.model.SagaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SagaRepository : JpaRepository<Saga, String> {
    fun findByStatus(status: SagaStatus): List<Saga>
    fun findByTypeAndStatus(type: String, status: SagaStatus): List<Saga>
    fun findByStartedAtBefore(startedAt: Instant): List<Saga>
    fun findByStatusInAndStartedAtBefore(statuses: List<SagaStatus>, startedAt: Instant): List<Saga>
}
