package com.vertyll.veds.notification.infrastructure.kafka.producer

import com.vertyll.veds.mail.mail.MailRequestedEvent
import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.infrastructure.kafka.NotificationKafkaTopics
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
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
                .setSubject(subjectFor(type))
                .setTemplateName(type.name)
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

    private fun subjectFor(type: NotificationType): String =
        when (type) {
            NotificationType.PROJECT_INVITATION -> "You have been invited to a project"
            NotificationType.PROJECT_MEMBER_JOINED -> "A new member joined your project"
            NotificationType.TASK_CREATED -> "A new task was created"
            NotificationType.TASK_ASSIGNED -> "A task was assigned to you"
            NotificationType.TASK_STATUS_CHANGED -> "A task changed status"
            NotificationType.TASK_COMMENT_ADDED -> "New comment on a task"
        }
}
