package com.vertyll.veds.notification.infrastructure.saga

import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.application.saga.model.SagaStepNames
import com.vertyll.veds.notification.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensator
import org.slf4j.LoggerFactory

internal class NotificationSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity, NotificationCompensationCommand> {
    private val logger = LoggerFactory.getLogger(NotificationSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext<NotificationCompensationCommand>,
    ) {
        val command =
            when (step.stepName) {
                SagaStepNames.PERSIST_NOTIFICATION.value ->
                    NotificationCompensationCommand.DeleteNotification(readNotificationId(context, step))
                SagaStepNames.PUBLISH_NOTIFICATION_EVENT.value ->
                    NotificationCompensationCommand.LogNotificationCompensation(readNotificationId(context, step))
                SagaStepNames.PROCESS_NOTIFICATION.value -> {
                    logger.info(
                        "No compensation needed for step '{}' on saga '{}' (effect not externally observable)",
                        step.stepName,
                        saga.id,
                    )
                    return
                }
                else -> {
                    logger.warn("No compensation defined for step '{}' on saga '{}'", step.stepName, saga.id)
                    return
                }
            }

        context.publishCompensationEvent(
            sagaId = saga.id,
            stepId = step.id,
            command = command,
        )
    }

    private fun readNotificationId(
        context: SagaCompensationContext<NotificationCompensationCommand>,
        step: SagaStepJpaEntity,
    ): String {
        val payload = context.readStepPayload(step.payload)
        val raw =
            payload["notificationId"]
                ?: error("Missing 'notificationId' in step payload for step ${step.id} (keys: ${payload.keys})")
        return raw.toString()
    }
}
