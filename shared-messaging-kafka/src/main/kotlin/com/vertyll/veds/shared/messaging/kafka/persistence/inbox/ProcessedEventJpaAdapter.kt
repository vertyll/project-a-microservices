package com.vertyll.veds.shared.messaging.kafka.persistence.inbox

import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEvent
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventFactory
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventRepositoryPort
import org.springframework.stereotype.Component

/**
 * Binds the persistence-agnostic inbox ports to JPA.
 *
 * Registered wherever the service scans this package. Every service that
 * consumes an event carries one.
 */
@Component
class ProcessedEventJpaAdapter(
    private val repository: ProcessedEventRepository,
) : ProcessedEventRepositoryPort,
    ProcessedEventFactory {
    override fun insert(processedEvent: ProcessedEvent): ProcessedEvent {
        val entity =
            processedEvent as? ProcessedEventEntity
                ?: ProcessedEventEntity(
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
    ): ProcessedEvent = ProcessedEventEntity(eventId = eventId, consumerGroup = consumerGroup)
}
