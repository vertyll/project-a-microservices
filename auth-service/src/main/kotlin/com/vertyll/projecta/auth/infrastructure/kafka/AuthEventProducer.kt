package com.vertyll.projecta.auth.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.event.user.UserProfileUpdatedEvent
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.kafka.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class AuthEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Sends a user registration event to the Kafka topic.
     */
    fun sendUserRegisteredEvent(event: UserRegisteredEvent) {
        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, event.eventId, eventJson)
        logger.info("Sent user registration event for: ${event.email}")
    }

    /**
     * Sends a mail requested event to the Kafka topic.
     */
    fun sendMailRequestedEvent(event: MailRequestedEvent) {
        val eventJson = objectMapper.writeValueAsString(event)
        val message = MessageBuilder
            .withPayload(eventJson)
            .setHeader(KafkaHeaders.KEY, event.eventId)
            .setHeader(KafkaHeaders.TOPIC, KafkaTopics.MAIL_REQUESTED)
            .setHeader("__TypeId__", "mailRequested")
            .build()
        kafkaTemplate.send(message)
        logger.info("Sent mail request to: ${event.to}")
    }

    /**
     * Sends a user profile updated event to the Kafka topic.
     */
    fun sendUserProfileUpdatedEvent(event: UserProfileUpdatedEvent) {
        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(KafkaTopics.USER_UPDATED, event.eventId, eventJson)
        logger.info("Sent user profile updated event for user: ${event.email}")
    }
}
