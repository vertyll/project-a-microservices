package com.vertyll.veds.shared.messaging.kafka.persistence.outbox

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * An outbox row is a message that has been promised but not yet sent. Its status is the only thing
 * deciding whether the poller picks it up again, so a wrong transition means either an event that
 * is never published or one that is published twice.
 */
class OutboxStateMachineTest {
    private fun testOutbox(
        status: OutboxStatus = OutboxStatus.PENDING,
        retryCount: Int = 0,
    ) = OutboxEntity(
        topic = "project-created",
        key = "project-1",
        payload = byteArrayOf(1, 2, 3),
        status = status,
        retryCount = retryCount,
    )

    @Test
    fun `a fresh row is pending, unsent and unretried`() {
        val message = testOutbox()

        assertEquals(OutboxStatus.PENDING, message.status)
        assertEquals(0, message.retryCount)
        assertNull(message.processedAt)
        assertNull(message.lastRetryAt)
    }

    /** Deduplication downstream keys off this id, so a row must never be created without one. */
    @Test
    fun `each row carries its own event id`() {
        assertTrue(testOutbox().eventId.isNotBlank())
        assertNotEquals(testOutbox().eventId, testOutbox().eventId)
    }

    /** Claiming a row is what stops a second poller from publishing the same message. */
    @Test
    fun `claiming a row marks it in flight`() {
        val message = testOutbox().markProcessing()

        assertEquals(OutboxStatus.PROCESSING, message.status)
        assertNotNull(message.processedAt)
    }

    @Test
    fun `a published row is completed and stamped`() {
        val message = testOutbox(status = OutboxStatus.PROCESSING).markCompleted()

        assertEquals(OutboxStatus.COMPLETED, message.status)
        assertNotNull(message.processedAt)
    }

    /**
     * A failed publishing goes back to PENDING — that is what makes the poller pick it up again. The
     * attempt counter and the timestamp are what stop it from being retried immediately and
     * forever.
     */
    @Test
    fun `a failed publish returns to the queue with the attempt counted`() {
        val message = testOutbox().markRetryScheduled("broker unavailable")

        assertEquals(OutboxStatus.PENDING, message.status)
        assertEquals(1, message.retryCount)
        assertEquals("broker unavailable", message.errorMessage)
        assertNotNull(message.lastRetryAt)
    }

    @Test
    fun `attempts accumulate across retries`() {
        val message =
            testOutbox()
                .markRetryScheduled("first")
                .markRetryScheduled("second")
                .markRetryScheduled("third")

        assertEquals(3, message.retryCount)
        assertEquals("third", message.errorMessage, "the newest failure should be the one on record")
    }

    /**
     * Dead-lettering is where automatic recovery stops. The row must leave PENDING, or the poller
     * would keep retrying a message that has already exhausted its attempts.
     */
    @Test
    fun `a dead-lettered row is out of the poller's reach`() {
        val message = testOutbox(retryCount = 5).markDeadLettered("still unavailable after 5 attempts")

        assertEquals(OutboxStatus.DEAD_LETTERED, message.status)
        assertEquals("still unavailable after 5 attempts", message.errorMessage)
        assertNotEquals(OutboxStatus.PENDING, message.status)
    }

    /** Nothing was published, so the row must not claim it was. */
    @Test
    fun `dead-lettering does not stamp the row as processed`() {
        assertNull(testOutbox().markDeadLettered("gave up").processedAt)
    }
}
