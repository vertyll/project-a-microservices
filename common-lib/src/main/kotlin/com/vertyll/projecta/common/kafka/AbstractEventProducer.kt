package com.vertyll.projecta.common.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.DomainEvent
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

abstract class AbstractEventProducer<T : DomainEvent>(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    abstract val topic: String

    fun send(event: T): CompletableFuture<SendResult<String, String>> {
        val eventJson = objectMapper.writeValueAsString(event)
        logger.info { "Sending event to topic $topic: $eventJson" }

        return kafkaTemplate.send(topic, event.eventId, eventJson)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info {
                        "Successfully sent event to topic ${result.recordMetadata.topic()}" +
                                " partition ${result.recordMetadata.partition()}" +
                                " offset ${result.recordMetadata.offset()}"
                    }
                } else {
                    logger.error(ex) { "Failed to send event to topic $topic" }
                }
            }
    }
} 