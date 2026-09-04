package com.vertyll.veds.shared.messaging.kafka.persistence.inbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data access to the idempotent-receiver ledger. A service enables it by
 * naming this package in `@EnableJpaRepositories`.
 */
@Repository
interface ProcessedEventRepository : JpaRepository<ProcessedEventEntity, Long> {
    fun existsByEventIdAndConsumerGroup(
        eventId: String,
        consumerGroup: String,
    ): Boolean
}
