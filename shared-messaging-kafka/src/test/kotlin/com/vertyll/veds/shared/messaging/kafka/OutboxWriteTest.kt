package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The write side of the outbox. It runs inside the caller's business transaction, so what it
 * records has to be everything the dispatcher will later need — the row is the only handover
 * between the transaction that produced the event and the poller that publishes it.
 */
internal class OutboxWriteTest {
    private val repository = InMemoryOutboxRepository()

    private val processor =
        KafkaOutboxProcessor(
            outboxRepository = repository,
            outboxMessageFactory = TestOutboxMessageFactory(),
            // Never used here: the write side only records the row. Publishing happens later, in a
            // separate transaction, and is covered by OutboxDispatchTest.
            kafkaTemplate = KafkaTemplate(DefaultKafkaProducerFactory(emptyMap())),
            properties = KafkaOutboxProperties(),
            dispatchTx = OutboxDispatchTx(repository),
        )

    @Test
    fun `a queued message starts pending and unretried`() {
        processor.saveOutboxMessage("project-created", "key-1", byteArrayOf(1, 2, 3))

        val message = repository.saved.single()
        assertEquals(OutboxStatus.PENDING, message.status)
        assertEquals(0, message.retryCount)
        assertNull(message.processedAt, "nothing is published inside the business transaction")
    }

    @Test
    fun `carries the topic, key and payload the dispatcher will publish`() {
        processor.saveOutboxMessage("project-created", "key-1", byteArrayOf(7, 8))

        val message = repository.saved.single()
        assertEquals("project-created", message.topic)
        assertEquals("key-1", message.key)
        assertContentEquals(byteArrayOf(7, 8), message.payload)
    }

    /**
     * The saga id is what lets compensation find the events a failed workflow already emitted, so
     * it has to survive the handover rather than be reconstructed later.
     */
    @Test
    fun `keeps the saga correlation on the row`() {
        processor.saveOutboxMessage("project-created", "key-1", byteArrayOf(1), sagaId = "saga-42")

        assertEquals("saga-42", repository.saved.single().sagaId)
        assertEquals(1, repository.findBySagaId("saga-42").size)
    }

    @Test
    fun `keeps a caller-supplied event id so consumers can deduplicate`() {
        processor.saveOutboxMessage("project-created", "key-1", byteArrayOf(1), eventId = "e-7")

        assertEquals("e-7", repository.saved.single().eventId)
        assertNotNull(repository.findByEventId("e-7"))
    }
}
