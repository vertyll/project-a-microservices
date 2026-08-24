package com.vertyll.veds.notification.infrastructure.kafka.producer

import com.vertyll.veds.mail.mail.MailRequestedEvent
import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.infrastructure.kafka.NotificationKafkaTopics
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.event.Events
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class KafkaMailRequestAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : MailRequestPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun requestMail(
        to: String,
        type: NotificationType,
        params: Map<String, String>,
    ) {
        val eventId = Events.newId()
        val event =
            MailRequestedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
                .setSubject(type.key)
                .setTemplateName(templateFor(type))
                .setVariables(params)
                .setReplyTo(null)
                .setPriority(0)
                .setSagaId(null)
                .build()

        val payload = avroPayloadSerializer.serialize(NotificationKafkaTopics.MAIL_REQUESTED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = NotificationKafkaTopics.MAIL_REQUESTED,
            key = to,
            payload = payload,
            sagaId = null,
            eventId = eventId,
        )
        logger.debug("Queued notification e-mail to {} for {}", to, type)
    }

    private fun templateFor(type: NotificationType): String = type.name.lowercase()
}
