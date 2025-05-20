package com.vertyll.projecta.mail.infrastructure.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.kafka.KafkaTopics
import com.vertyll.projecta.mail.domain.service.EmailService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class MailEventConsumer(
    private val objectMapper: ObjectMapper,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(topics = [KafkaTopics.MAIL_REQUESTED])
    fun consume(record: ConsumerRecord<String, String>, @Payload payload: String) {
        try {
            logger.info("Received mail request message: ${record.key()}")
            logger.debug("Message payload: ${record.value()}")
            
            // First try direct deserialization
            val event = try {
                objectMapper.readValue<MailRequestedEvent>(payload)
            } catch (e: JsonProcessingException) {
                logger.warn("Could not directly deserialize payload, attempting manual parsing: ${e.message}")
                // The payload may be wrapped in quotes, so we need to handle that
                val cleanPayload = if (payload.startsWith("\"") && payload.endsWith("\"")) {
                    // Remove the outer quotes and unescape any escaped quotes
                    payload.substring(1, payload.length - 1).replace("\\\"", "\"")
                } else {
                    payload
                }
                
                // Create MailRequestedEvent manually from the JSON
                val jsonNode = objectMapper.readTree(cleanPayload)
                createMailRequestedEventFromJson(jsonNode)
            }
            
            // Process the event
            handleEvent(event)
        } catch (e: Exception) {
            logger.error("Error processing message from topic ${record.topic()}", e)
            logger.error("Failed payload: $payload")
        }
    }
    
    private fun createMailRequestedEventFromJson(jsonNode: JsonNode): MailRequestedEvent {
        val variablesNode = jsonNode.get("variables")
        val variables = if (variablesNode != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                // Convert variables safely with type checking
                variablesNode.fields().asSequence().associate { (key, value) ->
                    key to (value.asText() ?: "")
                }
            } catch (e: Exception) {
                logger.warn("Could not convert variables to Map<String, String>, using empty map: ${e.message}")
                emptyMap()
            }
        } else {
            emptyMap()
        }
        
        return MailRequestedEvent(
            eventId = jsonNode.get("eventId")?.asText() ?: "",
            timestamp = try {
                objectMapper.convertValue(jsonNode.get("timestamp"), java.time.Instant::class.java)
            } catch (e: Exception) {
                logger.warn("Could not convert timestamp, using current time: ${e.message}")
                java.time.Instant.now()
            },
            eventType = jsonNode.get("eventType")?.asText() ?: "MAIL_REQUESTED",
            to = jsonNode.get("to")?.asText() ?: "",
            subject = jsonNode.get("subject")?.asText() ?: "",
            templateName = jsonNode.get("templateName")?.asText() ?: "",
            variables = variables,
            replyTo = jsonNode.get("replyTo")?.asText(),
            priority = jsonNode.get("priority")?.asInt() ?: 0,
            sagaId = jsonNode.get("sagaId")?.asText()
        )
    }
    
    private fun handleEvent(event: MailRequestedEvent) {
        logger.info("Processing mail request: ${event.eventId}")
        
        emailService.sendEmail(
            to = event.to,
            subject = event.subject,
            templateName = event.templateName,
            variables = event.variables
        )
        
        logger.info("Successfully processed mail request: ${event.eventId}")
    }
}
