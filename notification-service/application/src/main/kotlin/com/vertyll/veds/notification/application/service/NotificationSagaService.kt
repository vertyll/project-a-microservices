package com.vertyll.veds.notification.application.service

import com.vertyll.veds.notification.application.port.inbound.NotificationSagaUseCase
import com.vertyll.veds.notification.application.port.outbound.SagaProcessPort
import com.vertyll.veds.notification.application.saga.model.SagaStepNames
import com.vertyll.veds.notification.application.saga.model.SagaTypes
import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reference implementation of a saga-driven use case for the notification service.
 *
 * Mirrors the structure of `EmailSagaService` in mail-service — replace the
 * domain calls with your real business logic when cloning this service.
 */
@Service
internal class NotificationSagaService(
    private val sagaProcess: SagaProcessPort,
    private val notificationRepository: NotificationRepository,
) : NotificationSagaUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun processNotificationWithSaga(
        name: String,
        payload: String,
    ): Notification {
        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.NOTIFICATION_PROCESSING,
                    payload = mapOf("name" to name),
                ).id

        return try {
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_NOTIFICATION,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("name" to name),
            )

            val saved = notificationRepository.save(Notification(name = name, payload = payload))

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_NOTIFICATION,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("notificationId" to saved.id),
            )

            val processed = notificationRepository.save(saved.markProcessed())

            sagaProcess.markSagaCompleted(sagaId)
            processed
        } catch (e: Exception) {
            logger.error("Notification saga failed: ${e.message}", e)
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_NOTIFICATION,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: "Unknown error")),
            )
            sagaProcess.markSagaFailed(sagaId, e.message ?: "Unknown error")
            throw e
        }
    }
}
