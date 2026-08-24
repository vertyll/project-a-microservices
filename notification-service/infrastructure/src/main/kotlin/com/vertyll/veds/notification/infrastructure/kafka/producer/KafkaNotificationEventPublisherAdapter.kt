package com.vertyll.veds.notification.infrastructure.kafka.producer

import com.vertyll.veds.notification.NotificationFailedEvent
import com.vertyll.veds.notification.NotificationProcessedEvent
import com.vertyll.veds.notification.application.port.outbound.NotificationEventPublisherPort
import com.vertyll.veds.notification.infrastructure.kafka.NotificationKafkaTopics
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.event.Events
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class KafkaNotificationEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : NotificationEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishNotificationProcessed(
        sagaId: String,
        notificationId: Long,
        payload: Map<String, Any?>,
    ) {
        val eventId = Events.newId()
        val event =
            NotificationProcessedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setSagaId(sagaId)
                .setNotificationId(notificationId)
                .setPayload(payload.mapValues { it.value?.toString() ?: "" })
                .build()
        val bytes = avroPayloadSerializer.serialize(NotificationKafkaTopics.NOTIFICATION_PROCESSED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = NotificationKafkaTopics.NOTIFICATION_PROCESSED,
            key = eventId,
            payload = bytes,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.info("Published NOTIFICATION_PROCESSED for notificationId=$notificationId (sagaId=$sagaId)")
    }

    override fun publishNotificationFailed(
        sagaId: String,
        notificationId: Long?,
        error: String,
    ) {
        val eventId = Events.newId()
        val event =
            NotificationFailedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setSagaId(sagaId)
                .setNotificationId(notificationId)
                .setError(error)
                .build()
        val bytes = avroPayloadSerializer.serialize(NotificationKafkaTopics.NOTIFICATION_FAILED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = NotificationKafkaTopics.NOTIFICATION_FAILED,
            key = eventId,
            payload = bytes,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.info("Published NOTIFICATION_FAILED for notificationId=$notificationId (sagaId=$sagaId): $error")
    }
}
