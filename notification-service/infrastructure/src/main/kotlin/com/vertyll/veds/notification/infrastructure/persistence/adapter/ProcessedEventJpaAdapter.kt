package com.vertyll.veds.notification.infrastructure.persistence.adapter

import com.vertyll.veds.notification.infrastructure.persistence.entity.ProcessedEventJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.repository.ProcessedEventJpaRepository
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEvent
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventFactory
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventRepositoryPort
import org.springframework.stereotype.Component

@Component
internal class ProcessedEventJpaAdapter(
    private val repository: ProcessedEventJpaRepository,
) : ProcessedEventRepositoryPort,
    ProcessedEventFactory {
    override fun insert(processedEvent: ProcessedEvent): ProcessedEvent {
        val entity =
            processedEvent as? ProcessedEventJpaEntity
                ?: ProcessedEventJpaEntity(
                    eventId = processedEvent.eventId,
                    consumerGroup = processedEvent.consumerGroup,
                    processedAt = processedEvent.processedAt,
                )
        return repository.saveAndFlush(entity)
    }

    override fun exists(
        eventId: String,
        consumerGroup: String,
    ): Boolean = repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)

    override fun create(
        eventId: String,
        consumerGroup: String,
    ): ProcessedEvent = ProcessedEventJpaEntity(eventId = eventId, consumerGroup = consumerGroup)
}
