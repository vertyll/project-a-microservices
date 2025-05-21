package com.vertyll.projecta.common.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.DomainEvent
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload

private val logger = KotlinLogging.logger {}

abstract class AbstractEventConsumer<T : DomainEvent>(
    protected val objectMapper: ObjectMapper,
) {
    abstract val eventClass: Class<T>
    abstract val topicName: String

    /**
     * Consumes messages from the Kafka topic.
     * The topic name is generated based on the class name.
     */
    @KafkaListener(topics = ["#{@TOPIC_PREFIX}_#{T(java.util.Locale).ENGLISH.getDisplayLanguage()}"])
    protected fun consume(record: ConsumerRecord<String, String>, @Payload payload: String) {
        try {
            logger.info { "Received message: ${record.key()} - ${record.value()}" }
            val event = objectMapper.readValue(payload, eventClass)
            handleEvent(event)
        } catch (e: Exception) {
            logger.error(e) { "Error processing message from topic ${record.topic()}" }
        }
    }

    abstract fun handleEvent(event: T)

    /**
     * Generates the topic name based on the class name.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun getGeneratedTopicName(): String {
        return this.javaClass.simpleName
            .replace("Consumer", "")
            .replace("EventConsumer", "")
            .lowercase()
    }
}
