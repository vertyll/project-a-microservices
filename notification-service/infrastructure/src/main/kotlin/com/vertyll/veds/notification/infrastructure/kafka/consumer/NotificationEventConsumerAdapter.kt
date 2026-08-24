package com.vertyll.veds.notification.infrastructure.kafka.consumer

import com.vertyll.veds.notification.NotificationRequestedEvent
import com.vertyll.veds.notification.application.port.inbound.NotificationSagaUseCase
import com.vertyll.veds.notification.infrastructure.kafka.NotificationKafkaTopics
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter for `notification.requested`. Dedupes via
 * [ProcessedEventGuard] (idempotent receiver pattern).
 */
@Component
internal class NotificationEventConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val notificationSagaService: NotificationSagaUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "notification-service:notification-requested"
    }

    @KafkaListener(topics = [NotificationKafkaTopics.NOTIFICATION_REQUESTED])
    fun consume(
        record: ConsumerRecord<String, ByteArray>,
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (eventId != null && !processedEventGuard.claim(eventId, CONSUMER_GROUP)) {
            logger.info("Skipping duplicate event {} on {}", eventId, record.topic())
            return
        }
        try {
            logger.info("Received ${NotificationKafkaTopics.NOTIFICATION_REQUESTED} message: key={}", record.key())
            val event =
                avroPayloadDeserializer.deserialize(
                    NotificationKafkaTopics.NOTIFICATION_REQUESTED,
                    payload,
                ) as NotificationRequestedEvent
            val name = event.name
            val notificationPayload = event.payload ?: event.content ?: ""
            notificationSagaService.processNotificationWithSaga(name, notificationPayload)
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }
}
