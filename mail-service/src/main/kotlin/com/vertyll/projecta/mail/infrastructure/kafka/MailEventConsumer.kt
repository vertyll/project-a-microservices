package com.vertyll.projecta.mail.infrastructure.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import com.vertyll.projecta.common.mail.EmailTemplate
import com.vertyll.projecta.mail.domain.service.EmailService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MailEventConsumer(
    private val objectMapper: ObjectMapper,
    private val emailService: EmailService,
    private val kafkaTopicsConfig: KafkaTopicsConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        // Event field names
        private const val FIELD_EVENT_ID = "eventId"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_EVENT_TYPE = "eventType"
        private const val FIELD_TO = "to"
        private const val FIELD_SUBJECT = "subject"
        private const val FIELD_TEMPLATE_NAME = "templateName"
        private const val FIELD_VARIABLES = "variables"
        private const val FIELD_REPLY_TO = "replyTo"
        private const val FIELD_PRIORITY = "priority"
        private const val FIELD_SAGA_ID = "sagaId"
        
        // Default values
        private const val DEFAULT_EVENT_TYPE = "MAIL_REQUESTED"
        private const val DEFAULT_PRIORITY = 0
        
        // Log messages
        private const val LOG_RECEIVED_MESSAGE = "Received mail request message: {}"
        private const val LOG_MESSAGE_PAYLOAD = "Message payload: {}"
        private const val LOG_DESERIALIZATION_ERROR = "Could not directly deserialize payload, attempting manual parsing: {}"
        private const val LOG_VARIABLES_ERROR = "Could not convert variables to Map<String, String>, using empty map: {}"
        private const val LOG_TIMESTAMP_ERROR = "Could not convert timestamp, using current time: {}"
        private const val LOG_PROCESSING_ERROR = "Error processing message from topic {}"
        private const val LOG_FAILED_PAYLOAD = "Failed payload: {}"
        private const val LOG_PROCESSING_REQUEST = "Processing mail request: {}"
        private const val LOG_SUCCESS = "Successfully processed mail request: {}"
        private const val LOG_INVALID_TEMPLATE = "Received request with invalid template name: {}. Email will not be sent."
    }

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getMailRequestedTopic()}"])
    fun consume(record: ConsumerRecord<String, String>, @Payload payload: String) {
        try {
            logger.info(LOG_RECEIVED_MESSAGE, record.key())
            logger.debug(LOG_MESSAGE_PAYLOAD, record.value())

            // First try direct deserialization
            val event = try {
                objectMapper.readValue<MailRequestedEvent>(payload)
            } catch (e: JsonProcessingException) {
                logger.warn(LOG_DESERIALIZATION_ERROR, e.message)
                // The payload may be wrapped in quotes, so we need to handle that
                val cleanPayload = cleanJsonPayload(payload)

                // Create MailRequestedEvent manually from the JSON
                val jsonNode = objectMapper.readTree(cleanPayload)
                createMailRequestedEventFromJson(jsonNode)
            }

            handleEvent(event)
        } catch (e: Exception) {
            logger.error(LOG_PROCESSING_ERROR, record.topic(), e)
            logger.error(LOG_FAILED_PAYLOAD, payload)
        }
    }
    
    /**
     * Removes outer quotes and unescapes inner quotes if necessary
     */
    private fun cleanJsonPayload(payload: String): String {
        return if (payload.startsWith("\"") && payload.endsWith("\"")) {
            // Remove the outer quotes and unescape any escaped quotes
            payload.substring(1, payload.length - 1).replace("\\\"", "\"")
        } else {
            payload
        }
    }

    private fun createMailRequestedEventFromJson(jsonNode: JsonNode): MailRequestedEvent {
        val variablesNode = jsonNode.get(FIELD_VARIABLES)
        val variables = if (variablesNode != null) {
            try {
                // Convert variables safely with type checking
                variablesNode.fields().asSequence().associate { (key, value) ->
                    key to (value.asText() ?: "")
                }
            } catch (e: Exception) {
                logger.warn(LOG_VARIABLES_ERROR, e.message)
                emptyMap()
            }
        } else {
            emptyMap()
        }

        // Get the template name
        val templateName = jsonNode.get(FIELD_TEMPLATE_NAME)?.asText() ?: ""

        return MailRequestedEvent(
            eventId = jsonNode.get(FIELD_EVENT_ID)?.asText() ?: "",
            timestamp = try {
                objectMapper.convertValue(jsonNode.get(FIELD_TIMESTAMP), Instant::class.java)
            } catch (e: Exception) {
                logger.warn(LOG_TIMESTAMP_ERROR, e.message)
                Instant.now()
            },
            eventType = jsonNode.get(FIELD_EVENT_TYPE)?.asText() ?: DEFAULT_EVENT_TYPE,
            to = jsonNode.get(FIELD_TO)?.asText() ?: "",
            subject = jsonNode.get(FIELD_SUBJECT)?.asText() ?: "",
            templateName = templateName,
            variables = variables,
            replyTo = jsonNode.get(FIELD_REPLY_TO)?.asText(),
            priority = jsonNode.get(FIELD_PRIORITY)?.asInt() ?: DEFAULT_PRIORITY,
            sagaId = jsonNode.get(FIELD_SAGA_ID)?.asText()
        )
    }

    private fun handleEvent(event: MailRequestedEvent) {
        logger.info(LOG_PROCESSING_REQUEST, event.eventId)

        val template = EmailTemplate.fromTemplateName(event.templateName)
        
        if (template != null) {
            val success = emailService.sendEmail(
                to = event.to,
                subject = event.subject,
                template = template,
                variables = event.variables,
                replyTo = event.replyTo
            )
            
            if (success) {
                logger.info(LOG_SUCCESS, event.eventId)
            }
        } else {
            logger.error(LOG_INVALID_TEMPLATE, event.templateName)
        }
    }
}
