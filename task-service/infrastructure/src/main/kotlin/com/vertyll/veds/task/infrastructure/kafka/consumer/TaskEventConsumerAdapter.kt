package com.vertyll.veds.task.infrastructure.kafka.consumer

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard
import com.vertyll.veds.task.TaskRequestedEvent
import com.vertyll.veds.task.application.port.inbound.TaskSagaUseCase
import com.vertyll.veds.task.infrastructure.kafka.TaskKafkaTopics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter for `task.requested`. Dedupes via
 * [ProcessedEventGuard] (idempotent receiver pattern).
 */
@Component
internal class TaskEventConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val taskSagaService: TaskSagaUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "task-service:task-requested"
    }

    @KafkaListener(topics = [TaskKafkaTopics.TASK_REQUESTED])
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
            logger.info("Received ${TaskKafkaTopics.TASK_REQUESTED} message: key={}", record.key())
            val event = avroPayloadDeserializer.deserialize(TaskKafkaTopics.TASK_REQUESTED, payload) as TaskRequestedEvent
            val name = event.name
            val taskPayload = event.payload ?: event.content ?: ""
            taskSagaService.processTaskWithSaga(name, taskPayload)
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }
}
