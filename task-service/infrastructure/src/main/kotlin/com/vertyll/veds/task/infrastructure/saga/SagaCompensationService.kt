package com.vertyll.veds.task.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationEngine
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.infrastructure.config.SagaConfig
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaStepJpaEntity
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
internal class SagaCompensationService(
    private val sagaCompensationEngine: SagaCompensationEngine<SagaStepJpaEntity, TaskCompensationCommand>,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "task-service:saga-compensation"
    }

    @KafkaListener(topics = [SagaConfig.SAGA_COMPENSATION_TOPIC])
    fun handleCompensationEvent(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (eventId != null && !processedEventGuard.claim(eventId, CONSUMER_GROUP)) {
            logger.info("Skipping duplicate compensation event: eventId={}", eventId)
            return
        }
        sagaCompensationEngine.handleCompensationEvent(payload)
    }
}
