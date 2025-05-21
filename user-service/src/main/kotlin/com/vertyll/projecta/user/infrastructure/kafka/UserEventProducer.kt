package com.vertyll.projecta.user.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.user.CredentialsVerificationResultEvent
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.event.user.UserProfileUpdatedEvent
import com.vertyll.projecta.common.kafka.KafkaTopics
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class UserEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger: Logger = LoggerFactory.getLogger(UserEventProducer::class.java)

    fun send(event: UserRegisteredEvent): Boolean {
        try {
            // Set source to USER_SERVICE to avoid circular processing
            val modifiedEvent = event.copy(eventSource = "USER_SERVICE")

            val eventJson = objectMapper.writeValueAsString(modifiedEvent)
            kafkaTemplate.send(KafkaTopics.USER_REGISTERED, modifiedEvent.eventId, eventJson)

            logger.info("Sent user registered event for: ${modifiedEvent.email}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to send user registered event: ${e.message}", e)
            return false
        }
    }

    fun sendCredentialsVerificationResult(event: CredentialsVerificationResultEvent) {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            logger.info("Sending credentials verification result: $eventJson")
            kafkaTemplate.send(KafkaTopics.CREDENTIALS_VERIFICATION_RESULT, event.requestId, eventJson)
            logger.info("Sent credentials verification result for requestId: ${event.requestId}")
        } catch (e: Exception) {
            logger.error("Error sending credentials verification result: ${e.message}", e)
        }
    }

    fun send(event: UserProfileUpdatedEvent): Boolean {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(KafkaTopics.USER_UPDATED, event.eventId, eventJson)

            logger.info("Sent user profile updated event for: ${event.email}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to send user profile updated event: ${e.message}", e)
            return false
        }
    }
} 