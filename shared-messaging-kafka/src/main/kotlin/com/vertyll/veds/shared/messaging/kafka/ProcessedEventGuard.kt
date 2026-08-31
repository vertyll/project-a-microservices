package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventFactory
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Inbox-side idempotency: claims an event id for a consumer group so the same message is handled
 * once, however many times the broker delivers it.
 *
 * The claim is written **in the caller's transaction**, which is what makes the guard and the
 * retry/dead-letter machinery agree with each other:
 *
 * - the handler succeeds → claim and business writes commit together, and a redelivery is skipped;
 * - the handler fails → both roll back, so the redelivery is a real retry rather than a
 *   duplicate, and an exhausted message reaches the dead letter topic carrying its failure.
 *
 * A claim committed independently of the handler would invert that: every retry would find its
 * own claim, report a duplicate, and return successfully — so a transient failure would drop the
 * message on the first attempt and nothing would ever reach the dead letter topic.
 *
 * Delivery is therefore **at-least-once**, and a handler must tolerate being re-entered after a
 * failure. Genuine duplicates are still absorbed here.
 *
 * The caller must run inside a transaction — annotate the `@KafkaListener` method
 * `@Transactional` so the claim and the handler share one unit of work.
 */
@Service
class ProcessedEventGuard(
    private val repository: ProcessedEventRepositoryPort,
    private val factory: ProcessedEventFactory,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Claims [eventId] for [consumerGroup] within the caller's transaction.
     *
     * @return `true` when the caller should process the event, `false` when it was already
     * handled by this group.
     *
     * Two consumers racing on the same event both pass the existence check and one loses on the
     * unique constraint. That exception is deliberately not caught: it can only be resolved by
     * rolling back, and the redelivery then sees the committed row and skips cleanly.
     */
    fun claim(
        eventId: String,
        consumerGroup: String,
    ): Boolean {
        if (repository.exists(eventId, consumerGroup)) {
            logger.debug("Duplicate event detected: eventId={}, consumerGroup={}", eventId, consumerGroup)
            return false
        }
        repository.insert(factory.create(eventId = eventId, consumerGroup = consumerGroup))
        return true
    }
}
