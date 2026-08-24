package com.vertyll.veds.notification.infrastructure.saga

import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.saga.DeleteNotificationAction
import com.vertyll.veds.notification.saga.LogNotificationCompensationAction
import com.vertyll.veds.notification.saga.SagaCompensationEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.DecodedCompensationEvent

internal class AvroNotificationCompensationCommandTranslator(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationCommandDeserializer<NotificationCompensationCommand> {
    override fun deserialize(payload: ByteArray): DecodedCompensationEvent<NotificationCompensationCommand> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as SagaCompensationEvent
        val command =
            when (val action = record.action) {
                is DeleteNotificationAction ->
                    NotificationCompensationCommand.DeleteNotification(notificationId = action.notificationId.toString())
                is LogNotificationCompensationAction ->
                    NotificationCompensationCommand.LogNotificationCompensation(notificationId = action.notificationId.toString())
                else ->
                    error(
                        "Unknown compensation action type on saga-compensation-notification: ${action?.javaClass?.name}",
                    )
            }
        return DecodedCompensationEvent(
            sagaId = record.sagaId.toString(),
            stepId = record.stepId,
            command = command,
        )
    }
}
