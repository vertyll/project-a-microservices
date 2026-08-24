package com.vertyll.veds.file.infrastructure.persistence.repository

import com.vertyll.veds.file.infrastructure.persistence.entity.ProcessedEventJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface ProcessedEventJpaRepository : JpaRepository<ProcessedEventJpaEntity, Long> {
    fun existsByEventIdAndConsumerGroup(
        eventId: String,
        consumerGroup: String,
    ): Boolean
}
