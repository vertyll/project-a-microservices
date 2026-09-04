package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessage
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxRepositoryPort
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxEntity
import java.time.Instant

/**
 * A row that behaves exactly like a persisted one, because it is one: the transitions come from
 * [OutboxEntity] itself, so a test can never pass against a state machine the services do not
 * actually use.
 */
internal fun testOutboxMessage(
    id: Long? = null,
    eventId: String = "event-1",
    topic: String = "project-created",
    key: String = "project-1",
    payload: ByteArray = byteArrayOf(1),
    status: OutboxStatus = OutboxStatus.PENDING,
    retryCount: Int = 0,
    sagaId: String? = null,
) = OutboxEntity(
    id = id,
    eventId = eventId,
    topic = topic,
    key = key,
    payload = payload,
    status = status,
    retryCount = retryCount,
    sagaId = sagaId,
)

/**
 * Selection is deliberately explicit rather than a re-implementation of the SQL: which rows are
 * eligible is decided by `SELECT … FOR UPDATE SKIP LOCKED` and belongs to the integration tests.
 * What these fakes support is the decision the processor makes about a row it has already claimed.
 */
internal class InMemoryOutboxRepository : OutboxRepositoryPort {
    val saved = mutableListOf<OutboxMessage>()

    /**
     * The status each save committed, in order. A row is mutated in place — as a managed JPA entity
     * is — so reading `saved` afterward only ever shows the final state; the intermediate
     * transitions have to be captured as they happen.
     */
    val transitions = mutableListOf<Pair<String, OutboxStatus>>()

    /** Rows the next [lockBatchForDispatch] hands out, oldest promise first. */
    var claimable: List<OutboxMessage> = emptyList()

    override fun save(message: OutboxMessage) =
        message.also {
            saved += it
            transitions += it.eventId to it.status
        }

    override fun findByStatus(status: OutboxStatus) = saved.filter { it.status == status }

    override fun findBySagaId(sagaId: String) = saved.filter { it.sagaId == sagaId }

    override fun findByEventId(eventId: String) = saved.firstOrNull { it.eventId == eventId }

    override fun lockBatchForDispatch(
        maxRetries: Int,
        retriableBefore: Instant,
        stuckBefore: Instant,
        batchSize: Int,
    ) = claimable.take(batchSize)

    /** The last state each row was left in, which is what the next poll would see. */
    fun latest(eventId: String) = saved.last { it.eventId == eventId }
}

internal class TestOutboxMessageFactory : OutboxMessageFactory {
    override fun create(
        topic: String,
        key: String,
        payload: ByteArray,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage =
        testOutboxMessage(
            eventId = eventId ?: "generated-${counter++}",
            topic = topic,
            key = key,
            payload = payload,
            sagaId = sagaId,
        )

    private var counter = 0
}
