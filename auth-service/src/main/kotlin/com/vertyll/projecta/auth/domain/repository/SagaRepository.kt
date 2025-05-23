package com.vertyll.projecta.auth.domain.repository

import com.vertyll.projecta.auth.domain.model.Saga
import com.vertyll.projecta.auth.domain.model.SagaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SagaRepository : JpaRepository<Saga, String> {
    fun findByType(type: String): List<Saga>

    fun findByStatus(status: SagaStatus): List<Saga>

    fun findByTypeAndStatus(type: String, status: SagaStatus): List<Saga>

    fun findByIdAndType(id: String, type: String): Optional<Saga>
}
