package com.vertyll.projecta.sharedinfrastructure.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Service that processes messages from the Kafka outbox table and publishes them to Kafka.
 * This implements the Outbox Pattern for reliable event publishing.
 */
@Service
class KafkaOutboxProcessor(
    private val kafkaOutboxRepository: KafkaOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val MAX_RETRIES = 3
        private const val UNKNOWN_ERROR = "Unknown error"
    }

    /**
     * Scheduled job that processes pending messages from the outbox table
     */
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    @Transactional
    fun processOutboxMessages() {
        val pendingMessages =
            kafkaOutboxRepository.findMessagesToProcess(
                KafkaOutbox.OutboxStatus.PENDING,
                MAX_RETRIES,
            )

        logger.info("Found ${pendingMessages.size} pending messages to process")

        pendingMessages.forEach { message ->
            try {
                // Mark as processing using OL
                message.status = KafkaOutbox.OutboxStatus.PROCESSING
                message.processedAt = Instant.now()
                kafkaOutboxRepository.save(message)

                // Send to Kafka
                val result = kafkaTemplate.send(message.topic, message.key, message.payload).get()

                logger.info(
                    "Successfully sent message to Kafka: topic=${message.topic}, " +
                        "partition=${result.recordMetadata.partition()}, " +
                        "offset=${result.recordMetadata.offset()}",
                )

                // Mark as completed using OL
                message.status = KafkaOutbox.OutboxStatus.COMPLETED
                message.processedAt = Instant.now()
                kafkaOutboxRepository.save(message)
            } catch (e: Exception) {
                logger.error("Failed to process outbox message id=${message.id}: ${e.message}", e)

                // Mark as failed using OL
                message.status = KafkaOutbox.OutboxStatus.FAILED
                message.errorMessage = e.message ?: UNKNOWN_ERROR
                message.retryCount = message.retryCount + 1
                kafkaOutboxRepository.save(message)
            }
        }
    }

    /**
     * Creates a new outbox message and saves it to the database
     */
    @Transactional
    fun saveOutboxMessage(
        topic: KafkaTopicNames,
        key: String,
        payload: Any,
        sagaId: String? = null,
    ): KafkaOutbox {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val outboxMessage =
            KafkaOutbox(
                topic = topic.value,
                key = key,
                payload = payloadJson,
                sagaId = sagaId,
            )

        return kafkaOutboxRepository.save(outboxMessage)
    }
}
