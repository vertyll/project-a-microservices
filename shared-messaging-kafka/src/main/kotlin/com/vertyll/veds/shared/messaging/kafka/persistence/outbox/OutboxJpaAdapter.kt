@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.shared.messaging.kafka.persistence.outbox

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessage
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxRepositoryPort
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Binds the persistence-agnostic outbox ports to JPA.
 *
 * Registered wherever the service scans this package. A service that publishes
 * nothing carries no outbox table and excludes this bean along with the
 * processor, the way `translation-service` does.
 */
@Component
class OutboxJpaAdapter(
    private val repository: OutboxRepository,
) : OutboxRepositoryPort,
    OutboxMessageFactory {
    override fun save(message: OutboxMessage): OutboxMessage {
        val entity = message as? OutboxEntity ?: copyToEntity(message)
        return repository.save(entity)
    }

    override fun findByStatus(status: OutboxStatus): List<OutboxMessage> = repository.findByStatus(status)

    override fun findBySagaId(sagaId: String): List<OutboxMessage> = repository.findBySagaId(sagaId)

    override fun findByEventId(eventId: String): OutboxMessage? = repository.findByEventId(eventId)

    override fun lockBatchForDispatch(
        maxRetries: Int,
        retriableBefore: Instant,
        stuckBefore: Instant,
        batchSize: Int,
    ): List<OutboxMessage> =
        repository.lockBatchForDispatch(
            maxRetries = maxRetries,
            retriableBefore = retriableBefore,
            stuckBefore = stuckBefore,
            pageable = PageRequest.of(0, batchSize),
        )

    override fun create(
        topic: String,
        key: String,
        payload: ByteArray,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage =
        OutboxEntity(
            topic = topic,
            key = key,
            payload = payload,
            sagaId = sagaId,
            eventId = eventId ?: Uuid.generateV7().toString(),
        )

    private fun copyToEntity(message: OutboxMessage): OutboxEntity =
        OutboxEntity(
            id = message.id,
            eventId = message.eventId,
            topic = message.topic,
            key = message.key,
            payload = message.payload,
            status = message.status,
            errorMessage = message.errorMessage,
            createdAt = message.createdAt,
            processedAt = message.processedAt,
            retryCount = message.retryCount,
            lastRetryAt = message.lastRetryAt,
            sagaId = message.sagaId,
            version = message.version,
        )
}
