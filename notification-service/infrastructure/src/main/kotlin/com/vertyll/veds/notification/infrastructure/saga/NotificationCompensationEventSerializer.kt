package com.vertyll.veds.notification.infrastructure.saga

import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.saga.DeleteNotificationAction
import com.vertyll.veds.notification.saga.LogNotificationCompensationAction
import com.vertyll.veds.notification.saga.SagaCompensationEvent
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer

internal class NotificationCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer<NotificationCompensationCommand> {
    override fun serialize(
        sagaId: String,
        stepId: Long?,
        command: NotificationCompensationCommand,
    ): ByteArray {
        val action: Any =
            when (command) {
                is NotificationCompensationCommand.DeleteNotification ->
                    DeleteNotificationAction.newBuilder().setNotificationId(command.notificationId).build()
                is NotificationCompensationCommand.LogNotificationCompensation ->
                    LogNotificationCompensationAction.newBuilder().setNotificationId(command.notificationId).build()
            }
        val record =
            SagaCompensationEvent
                .newBuilder()
                .setSagaId(sagaId)
                .setStepId(stepId)
                .setAction(action)
                .build()
        return avroPayloadSerializer.serialize(topic, record)
    }
}
