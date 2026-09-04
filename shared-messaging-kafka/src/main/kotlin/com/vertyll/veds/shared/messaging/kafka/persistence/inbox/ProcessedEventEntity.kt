package com.vertyll.veds.shared.messaging.kafka.persistence.inbox

import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * A claim in the idempotent-receiver ledger: one consumer group has handled one
 * event.
 *
 * The row is inserted inside the handler's own transaction, so a handler that
 * fails leaves nothing claimed and the redelivery is a real retry rather than a
 * silent skip. The unique constraint is what makes the claim atomic under
 * concurrent delivery — the second insert loses.
 */
@Entity
@Table(
    name = "processed_event",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_processed_event_event_id_consumer",
            columnNames = ["event_id", "consumer_group"],
        ),
    ],
)
class ProcessedEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null,
    @Column(nullable = false)
    override var eventId: String,
    @Column(nullable = false)
    override var consumerGroup: String,
    @Column(nullable = false)
    override var processedAt: Instant = Instant.now(),
) : ProcessedEvent
