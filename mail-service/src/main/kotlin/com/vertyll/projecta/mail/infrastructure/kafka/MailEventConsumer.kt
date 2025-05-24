package com.vertyll.projecta.mail.infrastructure.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vertyll.projecta.common.event.EventType
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import com.vertyll.projecta.common.mail.EmailTemplate
import com.vertyll.projecta.mail.domain.service.EmailSagaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MailEventConsumer(
    private val objectMapper: ObjectMapper,
    private val emailSagaService: EmailSagaService,
    @Suppress("unused") private val kafkaTopicsConfig: KafkaTopicsConfig
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
        private val DEFAULT_EVENT_TYPE = EventType.MAIL_REQUESTED.value
        private const val DEFAULT_PRIORITY = 0

        // Log messages
        private const val MSG_RECEIVED = "Received mail request message: {}"
        private const val MSG_PAYLOAD = "Message payload: {}"
        private const val ERR_DESERIAL = "Could not directly deserialize payload, attempting manual parsing: {}"
        private const val ERR_VARIABLES = "Could not convert variables to Map<String, String>, using empty map: {}"
        private const val ERR_TIMESTAMP = "Could not convert timestamp, using current time: {}"
        private const val ERR_PROCESSING = "Error processing message from topic {}"
        private const val ERR_FAILED_PAYLOAD = "Failed payload: {}"
        private const val MSG_PROCESSING = "Processing mail request: {}"
        private const val MSG_SUCCESS = "Successfully processed mail request: {}"
        private const val ERR_INV_TEMPLATE = "Received request with invalid template name: {}. Email will not be sent."
    }

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getMailRequestedTopic()}"])
    fun consume(record: ConsumerRecord<String, String>, @Payload payload: String) {
        try {
            logger.info(MSG_RECEIVED, record.key())
            logger.debug(MSG_PAYLOAD, record.value())

            // First try direct deserialization
            val event = try {
                objectMapper.readValue<MailRequestedEvent>(payload)
            } catch (e: JsonProcessingException) {
                logger.warn(ERR_DESERIAL, e.message)
                // The payload may be wrapped in quotes, so we need to handle that
                val cleanPayload = cleanJsonPayload(payload)

                // Create MailRequestedEvent manually from the JSON
                val jsonNode = objectMapper.readTree(cleanPayload)
                createMailRequestedEventFromJson(jsonNode)
            }

            handleEvent(event)
        } catch (e: Exception) {
            logger.error(ERR_PROCESSING, record.topic(), e)
            logger.error(ERR_FAILED_PAYLOAD, payload)
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
                logger.warn(ERR_VARIABLES, e.message)
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
                logger.warn(ERR_TIMESTAMP, e.message)
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
        logger.info(MSG_PROCESSING, event.eventId)

        val template = EmailTemplate.fromTemplateName(event.templateName)

        if (template != null) {
            // Use EmailSagaService to track the email sending process
            val success = emailSagaService.sendEmailWithSaga(
                to = event.to,
                subject = event.subject,
                template = template,
                variables = event.variables,
                replyTo = event.replyTo
            )

            if (success) {
                logger.info(MSG_SUCCESS, event.eventId)
            }
        } else {
            logger.error(ERR_INV_TEMPLATE, event.templateName)
        }
    }
}
